# Crypto Challenge

Solution to the Crypto Challenge. The challenge was to implement the following endpoints:

* createAccount(name, usd_balance): Creates an account on the application with 0 BTC.
* fetchAccountDetails(account_id): Fetches account details.
* createLimitOrder(account_id, price_limit): Creates a limit order, waiting to be executed when the price limit is reached.
* fetchOrderDetails(order_id): Fetches order details and status.

Further, it should be possible to execute an order if the price falls below the limit price.

The solution uses Postgresql as the DB, Vertx for the development of the async services and Junit 4 for unit tests. The build is managed by Maven.

## Getting Started

All the source for the solution can be found under:

[src/main](https://github.com/msalter1/crypto/tree/master/src/main/java/msalter/crypto) src/main - the source  
[src/test](https://github.com/msalter1/crypto/tree/master/src/test/java/msalter/crypto) - Junit tests  
[sql](https://github.com/msalter1/crypto/tree/master/sql) - DB DDL scripts  
[bin](https://github.com/msalter1/crypto/tree/master/bin) - fully executable jars (including all dependencies)  
[doc](https://github.com/msalter1/crypto/tree/master/doc) - Javadocs  

### Prerequisites

Following is required:

* [postgresql](https://www.postgresql.org/)

### Installing

Database configuration:

A DB user 'postgres' must already exist. The DB configuration is in the app's config.properties' file - 

     host=localhost  
     port=5432  
     username=postgres  
     password=Portree123   

Execute following scripts:

```
CREATE_DB.sql
CREATE_DB_Objects.sql
```
This will create a database called 'crypto' with owner 'postgres'

## Running the services

### Delete old test data

Execute following script to remove old test data:  

```
DELETE_All_Data.sql
```

### Start the dummy BTC Service 

From the command line launch python BTC Service:  

```
python exchange.py
```

### Start the Crypto services  

Either run 'App' from eclipse, or run the CryptoAppFull.jar directly:  

```
java -jar CryptoAppFull.jar
```
Note : the config.properties file must be in the same folder as the jar

### Test the services

createAccount(name, usd_balance):  
```
curl -v -X POST --data "name=Crypto Fund AG&usd_balance=50000" http://127.0.0.1:8081/account
```

fetchAccountDetails(account_id):

```
curl -v -X GET http://127.0.0.1:8081/account/2
```
createLimitOrder(account_id, price_limit):  
```
curl -v -X POST --data "account_id=2&price_limit=3124.12" http://127.0.0.1:8081/limitorder
```

fetchOrderDetails(order_id): 
```
curl -v -X GET http://127.0.0.1:8081/orderdetails/1
```
## Executing a limit order

Either run 'ExecuteLimitOrders' from eclipse, or run the ExecuteLimitOrdersFull.jar directly:  

```
java -jar ExecuteLimitOrdersFull.jar
```
Successfully processed limit orders will have their status set to 'processed' in the client_order table

## Running the Junit tests

The JUnit 4 tests are incomplete - there is currently just one test to illustrate how async services can be tested and this is currently disabled because there was insufficient time to write the setup and teardown code. 

From Eclipse just run the Maven 'test' goal, or use the mvn command line.


## Built With

* [Vertx](https://vertx.io/) - The web framework used
* [Maven](https://maven.apache.org/) - Dependency Management
* [Postgresql](https://www.postgresql.org/) - Database
