
/*
-- =============================================
-- Author:      Mark Salter
-- Create date: 
-- Description: Script to initialise DB for crypto exercise
-- =============================================
*/

--
-- Types
--
-- order status
DROP TYPE IF EXISTS order_status_type CASCADE;
CREATE TYPE order_status_type AS ENUM (
	'created', -- initial, default state
    'processed',
    'cancelled' 
);

-- basic transaction type
DROP TYPE IF EXISTS transaction_type_type CASCADE;
CREATE TYPE transaction_type_type AS ENUM (
	'buy',
    'sell'
);

-- order types
DROP TYPE IF EXISTS order_type_type CASCADE;
CREATE TYPE order_type_type AS ENUM (
	'market', -- default
    'limit',
	'stop'
);

--
-- Tables
--
-- client
DROP TABLE IF EXISTS client CASCADE ;
CREATE TABLE client
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	name VARCHAR(100) NOT NULL UNIQUE,
	created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE client OWNER to postgres;

-- acount
DROP TABLE IF EXISTS account CASCADE ;
CREATE TABLE account
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	client_id BIGINT REFERENCES client(id) NOT NULL,
	name VARCHAR(100) NOT NULL,
	ccy CHAR(3) NOT NULL,
	is_trading_account BOOLEAN NOT NULL,
	default_settlement_account_id BIGINT REFERENCES account(id),
	created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE account OWNER to postgres;


-- balance
DROP TABLE IF EXISTS account_balance CASCADE ;
CREATE TABLE account_balance
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	account_id BIGINT REFERENCES account(id) NOT NULL,
	balance_date DATE NOT NULL DEFAULT CURRENT_DATE,
	balance DECIMAL(27,18), -- yes, 18 dec places for ETH (!)
	created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE account_balance OWNER to postgres;

-- order
DROP TABLE IF EXISTS client_order CASCADE ;
CREATE TABLE client_order
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	account_id BIGINT REFERENCES account(id) NOT NULL, -- the trading account (ie XBT)
	settlement_account_id BIGINT REFERENCES account(id) NOT NULL, -- the funding account (ie USD)
	transaction_type transaction_type_type NOT NULL, -- buy / sell
	amount DECIMAL(27,18) NOT NULL,
	order_type order_type_type,
	limit_price DECIMAL (19, 9),
	stop_price DECIMAL (19, 9),
	execution_date DATE,
	execution_price DECIMAL (19, 9),
	settlement_amount DECIMAL(27,18),
	status order_status_type DEFAULT 'created' NOT NULL,
	created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE client_order OWNER to postgres;


-- entry
DROP TABLE IF EXISTS entry CASCADE ;
CREATE TABLE entry
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
	account_id BIGINT REFERENCES account(id) NOT NULL,
	value_date DATE NOT NULL DEFAULT CURRENT_DATE,
	amount DECIMAL(27,18), -- yes, 18 dec places for ETH 
	order_id BIGINT REFERENCES client_order(id),
	created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE entry OWNER to postgres;

--
-- Functions and SPs
--

-- account details type
DROP TYPE IF EXISTS account_details CASCADE;
CREATE TYPE account_details AS (
	id BIGINT, 
	name VARCHAR(100), 
	ccy CHAR(3),
	balance DECIMAL(27,18),
	is_trading_account BOOLEAN,
	default_settlement_account_id BIGINT,
	created TIMESTAMP
);

-- entry details type
DROP TYPE IF EXISTS entry_details CASCADE;
CREATE TYPE entry_details AS (
	id BIGINT, 
	account_id BIGINT,
	value_date DATE,
	amount DECIMAL(27,18),
	order_id BIGINT,
	created TIMESTAMP
);

-- order details type
DROP TYPE IF EXISTS order_details CASCADE;
CREATE TYPE order_details AS (
	id BIGINT, 
	account_id BIGINT,
	settlement_account_id BIGINT,
	client_id BIGINT,
	transaction_type transaction_type_type,
	ccy CHAR(3), 
	amount DECIMAL(27,18),
	order_type order_type_type,
	limit_price DECIMAL (19, 9),
	stop_price DECIMAL (19, 9),
	execution_date DATE,
	execution_price DECIMAL (19, 9),
	settlement_ccy CHAR(3), 
	settlement_amount DECIMAL(27,18),
	status order_status_type,
	created TIMESTAMP
);

DROP FUNCTION IF EXISTS fetch_account_details;
CREATE OR REPLACE FUNCTION fetch_account_details (account_id BIGINT)
RETURNS SETOF account_details AS $$
#variable_conflict use_variable
DECLARE 
	--xxx INT;
BEGIN
	-- TODO : implement existence check here
	RETURN QUERY SELECT 
		account.ID, 
		account.name, 
		account.ccy, 
		COALESCE(
			(SELECT balance FROM account_balance WHERE account_balance.account_id = account_id ORDER BY balance_date DESC LIMIT 1),
			CAST(0 as NUMERIC(27,18))) AS balance,
		account.is_trading_account, 
		account.default_settlement_account_id, 
		account.created 
		FROM account 
		WHERE account.id = account_id;
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS fetch_entry_details;
CREATE OR REPLACE FUNCTION fetch_entry_details (entry_id BIGINT)
RETURNS SETOF entry_details AS $$
#variable_conflict use_variable
BEGIN
	-- TODO : implement existence check here
	RETURN QUERY SELECT 
		id, 
		account_id,
		value_date,
		amount,
		order_id,
		created 
		FROM entry 
		WHERE entry.id = entry_id;
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS create_entry;
CREATE OR REPLACE FUNCTION create_entry (account_id BIGINT, value_date DATE, amount DECIMAL(27,18), order_id BIGINT)
RETURNS SETOF entry_details AS $$
DECLARE 
	new_entry_id BIGINT;
BEGIN

	INSERT INTO entry(		
		account_id,
		value_date,
		amount,
		order_id
	) VALUES (
		account_id,
		value_date,
		amount	,
		order_id
	) RETURNING ID INTO new_entry_id;

	RETURN QUERY SELECT * FROM fetch_entry_details(new_entry_id);
	
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS revalue_account;
CREATE OR REPLACE FUNCTION revalue_account (account_id BIGINT, value_date DATE)
RETURNS VOID AS $$
#variable_conflict use_variable
DECLARE 
	current_balance DECIMAL(27,18);
	cur_entry CURSOR FOR 	
		SELECT account_balance.id AS balance_id, entry.account_id, entry.value_date, SUM(entry.amount) AS amount FROM entry 
		LEFT OUTER JOIN account_balance ON account_balance.account_id = entry.account_id
		WHERE entry.account_id = account_id AND entry.value_date >= value_date
		GROUP BY account_balance.id, entry.account_id, entry.value_date ORDER BY entry.value_date DESC;
	rec_entry   RECORD;
BEGIN

	-- get most recent balance	
	SELECT COALESCE(
			(SELECT balance FROM account_balance WHERE account_balance.account_id = account_id AND balance_date < value_date ORDER BY balance_date DESC LIMIT 1),
			CAST(0 as NUMERIC(27,18))) INTO current_balance;

 	-- go through entries inserting / updating balances 
	OPEN cur_entry;
   
	LOOP
    	-- fetch row into the film
      	FETCH cur_entry INTO rec_entry;
		
    	-- exit when no more row to fetch
      	EXIT WHEN NOT FOUND;
 
 		-- update current balance
		current_balance = current_balance + rec_entry.amount;
		
      	IF rec_entry.balance_id IS NULL THEN 
         	RAISE NOTICE 'missing balance';
		 
		 	-- create new balance
			INSERT INTO account_balance(		
				account_id,
				balance_date,
				balance
			) VALUES (
				rec_entry.account_id,
				rec_entry.value_date,
				current_balance
			);		 
	 ELSE
         RAISE NOTICE 'existing balance';	
		 
		 -- update existing balance
		 UPDATE account_balance SET balance = current_balance WHERE id = rec_entry.balance_id;
		 
      END IF;
   END LOOP;
  
   -- Close the cursor
   CLOSE cur_entry;			
	
END;
$$ LANGUAGE plpgsql;



DROP FUNCTION IF EXISTS create_account;
CREATE OR REPLACE FUNCTION create_account (xname VARCHAR(100) , usd_balance DECIMAL(27,18))
RETURNS SETOF account_details AS $$
DECLARE 
new_client_id BIGINT;
new_settlement_account_id BIGINT;
new_trading_account_id BIGINT;
BEGIN

	--create a new client with a unique name - if the name already exists an exception will be thrown
	-- TODO : implement duplicate check here
	
	-- create a client
	-- TODO : move this out into own function
	INSERT INTO client(name) VALUES (xname) RETURNING ID INTO new_client_id;
	
	-- create 2 accounts, one funding account for USD and one trading account for XBT
	
	-- USD account
	INSERT INTO account(client_id, name, ccy, is_trading_account, default_settlement_account_id) VALUES (new_client_id, xname, 'USD',false, NULL) RETURNING ID INTO new_settlement_account_id;
  -- XBT account
	INSERT INTO account(client_id, name, ccy, is_trading_account, default_settlement_account_id) VALUES (new_client_id, xname, 'XBT',true, new_settlement_account_id) RETURNING ID INTO new_trading_account_id;
	
	-- post opening balance entries
	
	-- USD opening balance
	PERFORM create_entry(new_settlement_account_id, CURRENT_DATE, usd_balance, NULL);
	-- XBT opening balance
	PERFORM create_entry(new_trading_account_id, CURRENT_DATE, 0, NULL);
	
	-- revalue account balances
	PERFORM revalue_account(new_settlement_account_id, CURRENT_DATE);
	PERFORM revalue_account(new_trading_account_id, CURRENT_DATE);
	
	-- return account details of XBT account
	RETURN QUERY SELECT * FROM fetch_account_details(new_trading_account_id);
	
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS fetch_order_details;
CREATE OR REPLACE FUNCTION fetch_order_details (order_id BIGINT)
RETURNS SETOF order_details AS $$
BEGIN
	-- TODO : implement existence check here
	RETURN QUERY SELECT	
		client_order.id AS id,
		account_id,
		settlement_account_id,
		client_account.client_id AS client_id,
		transaction_type,
		client_account.ccy AS ccy, 
		amount,
		order_type,
		limit_price,
		stop_price,
		execution_date,
		execution_price,
		client_settlement_account.ccy AS settlement_ccy, 
		settlement_amount,
		client_order.status AS status,
		client_order.created AS created	
	FROM client_order 
	LEFT OUTER JOIN account AS client_account ON client_account.id = account_id
	LEFT OUTER JOIN account AS client_settlement_account ON client_settlement_account.id = settlement_account_id
	WHERE client_order.id = order_id;
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS create_limit_order;
CREATE OR REPLACE FUNCTION create_limit_order (account_id BIGINT, price_limit DECIMAL (19, 9)) -- but where is amount?
RETURNS SETOF order_details AS $$
DECLARE 
	new_order_id BIGINT;
BEGIN
	-- TODO : implement basic validation checks
	
	INSERT INTO client_order(
		account_id,
		settlement_account_id,
		transaction_type,
		amount,
		order_type,
		limit_price,
		stop_price,
		execution_date,
		execution_price,
		settlement_amount,
		status
	) VALUES (
		account_id,
		(SELECT default_settlement_account_id FROM account WHERE id = account_id LIMIT 1),
		'buy',
		CAST(50 as numeric(27,18)),
		'limit',
		price_limit,
		NULL,
		NULL,
		NULL,
		NULL,
		'created'
	) RETURNING ID INTO new_order_id;
	
	RETURN QUERY SELECT * FROM fetch_order_details(new_order_id);
	
END;
$$ LANGUAGE plpgsql;


DROP FUNCTION IF EXISTS execute_limit_order;
CREATE OR REPLACE FUNCTION execute_limit_order (order_id BIGINT, price_execution DECIMAL (19, 9)) 
RETURNS SETOF order_details AS $$
DECLARE 
	o_details order_details;
BEGIN
	-- TODO : implement existence check here
	-- TODO : basic validation checks including sufficient funds 
	
	-- update order	
	UPDATE client_order SET 
	execution_date = CURRENT_DATE, 
	execution_price = price_execution, 
	settlement_amount = amount * price_execution, 
	status = 'processed'
	WHERE id = order_id;
	
	-- fetch order 
	SELECT INTO o_details * FROM fetch_order_details(order_id);	
	
	-- post entries
	
	-- USD opening balance
	PERFORM create_entry(o_details.settlement_account_id, o_details.execution_date, o_details.settlement_amount, order_id);
	-- XBT opening balance
	PERFORM create_entry(o_details.account_id, o_details.execution_date, o_details.amount, order_id);
	
	-- revalue account balances
	PERFORM revalue_account(o_details.settlement_account_id, o_details.execution_date);
	PERFORM revalue_account(o_details.account_id, o_details.execution_date);	

	RETURN QUERY SELECT * FROM fetch_order_details(order_id);
	
END;
$$ LANGUAGE plpgsql;

--
-- Tests
--
-- Test functions
DROP FUNCTION IF EXISTS test_create_account;
CREATE OR REPLACE FUNCTION test_create_account()
RETURNS BIGINT AS $$
DECLARE ac_details account_details;
BEGIN
	SELECT INTO ac_details * FROM create_account('Crypto Fund AG', 500000.0);
	RETURN(ac_details.id);
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS test_fetch_account_details;
CREATE OR REPLACE FUNCTION test_fetch_account_details()
RETURNS BIGINT AS $$
DECLARE 
	ac_details account_details;
	account_id BIGINT;
BEGIN
	SELECT account.id  INTO account_id FROM account WHERE is_trading_account = true AND account.name = 'Crypto Fund AG' LIMIT 1;
	SELECT INTO ac_details * FROM fetch_account_details(account_id);
	RETURN(ac_details.id);
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS test_create_limit_order;
CREATE OR REPLACE FUNCTION test_create_limit_order()
RETURNS BIGINT AS $$
DECLARE 
	o_details order_details;
	account_id BIGINT;
BEGIN
	SELECT account.id  INTO account_id FROM account WHERE is_trading_account = true AND account.name = 'Crypto Fund AG' LIMIT 1;
	SELECT INTO o_details * FROM create_limit_order(account_id, CAST(8719.16 AS DECIMAL(19, 9)));
	RETURN(o_details.id);
END;
$$ LANGUAGE plpgsql;


DROP FUNCTION IF EXISTS test_fetch_order_details;
CREATE OR REPLACE FUNCTION test_fetch_order_details()
RETURNS BIGINT AS $$
DECLARE 
	o_details order_details;
	order_id BIGINT;
BEGIN
	SELECT id INTO order_id FROM client_order WHERE id = (SELECT id FROM client WHERE name = 'Crypto Fund AG' LIMIT 1) LIMIT 1;
	SELECT INTO o_details * FROM fetch_order_details(order_id);
	RETURN(o_details.id);
END;
$$ LANGUAGE plpgsql;

DROP FUNCTION IF EXISTS test_execute_limit_order;
CREATE OR REPLACE FUNCTION test_execute_limit_order()
RETURNS BIGINT AS $$
DECLARE o_details order_details;
order_id BIGINT;
BEGIN
	SELECT id INTO order_id FROM client_order WHERE id = (SELECT id FROM client WHERE name = 'Crypto Fund AG' LIMIT 1) LIMIT 1;
	SELECT INTO o_details * FROM execute_limit_order(order_id, CAST(8720.16 AS DECIMAL(19, 9)));
	RETURN(o_details.id);
END;
$$ LANGUAGE plpgsql;

/*
-- Execute Tests
SELECT test_create_account();
SELECT test_fetch_account_details();
SELECT test_create_limit_order();
SELECT test_fetch_order_details();
SELECT test_execute_limit_order();
*/

