
/*
-- =============================================
-- Author:      Mark Salter
-- Create date: 
-- Description: Script to create DB for crypto exercise
-- =============================================
*/

-- Database: crypto
-- NOTE : CREATE DATABASE cannot run inside a transaction block

DROP DATABASE IF EXISTS crypto;

CREATE DATABASE crypto
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'German_Switzerland.1252'
    LC_CTYPE = 'German_Switzerland.1252'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

COMMENT ON DATABASE crypto
    IS 'Crypto exercise database';
