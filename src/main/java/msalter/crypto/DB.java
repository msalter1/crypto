package msalter.crypto;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.asyncsql.PostgreSQLClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

/**
 * <p>
 * Data Access Layer
 * </p>
 * @author      Mark Salter <a href="mailto:msalter@swissonline.ch">msalter@swissonline.ch</a>
 * @version     0.1
 */
public class DB {

	/**
	 * Connect to the PostgreSQL database
	 * @param vertx     the Vertx object
     * @param handler   handler to process the async result
	 */
	public static void getConnection(Vertx vertx, Handler<AsyncResult<SQLConnection>> handler) {

		JsonObject postgreSQLClientConfig = new JsonObject()
				.put("host", "localhost")
				.put("port", 5432)
				.put("username", "postgres")
				.put("password", "Portree123")
				.put("database", "crypto");

		SQLClient postgreSQLClient = PostgreSQLClient.createShared(vertx, postgreSQLClientConfig);     

		postgreSQLClient.getConnection(res -> {

			handler.handle(res);

		});  

	}

	/**
	 * Execute an sql statement with optional parameters. Any sql exceptions will be handled here and wrapped in Result
	 * 
	 * @param sql     	the sql statement to be executed
     * @param params   	an array of parameters which may be null or empty
	 * @param vertx     the Vertx object
     * @param handler   a handler to process the async result
     * @param resultCreator   a method to create a Result for the data returned in AsyncResult
     * 
	 * NOTE : Use of Vert.x SQL 'call', 'callWithParams' to call functions does not seem to work (Not Implemented) for postgresql
	 */
	public static void execSQL(String sql, JsonArray params, Vertx vertx, Handler<Result> handler, DataResultCreator resultCreator) {

		try {		

			// get DB connection
			DB.getConnection(vertx, res -> {				

				if (res.succeeded()) {

					// got connection
					SQLConnection connection = res.result();

					if (params == null || params.size() == 0) {

						// run query - without params					

						connection.query(sql,   ar -> {

							// close connection
							connection.close();

							// return result by calling handler with standard Result object wrapping ResultSet data
							handler.handle( resultCreator.createResult(ar));

						});    	    

					}
					else {

						// run query - with params					

						connection.queryWithParams(sql, params,  ar -> {

							
							// close connection
							connection.close();

							// return result by calling handler with standard Result object wrapping ResultSet data
							handler.handle( resultCreator.createResult(ar));

						});    	    

					}

				} else {

					// Failed to get connection

					// return result by calling handler with standard Result object wrapping failure
					handler.handle( new Result(res, false));

				}				

			});     

		} catch (Exception e) {

			// return result by calling handler with standard Result object wrapping exception message
			handler.handle(new Result(e));

		} 			

	}

	/**
	 * Execute an SQL statement and return multiple rows
	 * @param sql     	the sql statement to be executed
     * @param params   	an array of parameters which may be null or empty
	 * @param vertx     the Vertx object
     * @param handler   a handler to process the async result
	 */
	public static void execAndReturnRows(String sql, JsonArray params, Vertx vertx, Handler<Result> handler) {

		DB.execSQL(sql, params, vertx, handler, ar -> {
			return Result.createForRowsData(ar);
		} ) ;

	}

	/**
	 * Execute an SQL statement and return a single row
	 * @param sql     	the sql statement to be executed
     * @param params   	an array of parameters which may be null or empty
	 * @param vertx     the Vertx object
     * @param handler   a handler to process the async result
	 */
	public static void execAndReturnRow(String sql, JsonArray params, Vertx vertx, Handler<Result> handler) {

		DB.execSQL(sql, params, vertx, handler, ar -> {
			return Result.createForRowData(ar);
		} ) ;

	}

	/**
	 * Execute an SQL statement and return a single value
	 * @param sql     	the sql statement to be executed
     * @param params   	an array of parameters which may be null or empty
	 * @param vertx     the Vertx object
     * @param handler   a handler to process the async result
	 */
	public static void execAndReturnValue(String sql, JsonArray params, Vertx vertx, Handler<Result> handler) {

		DB.execSQL(sql, params, vertx, handler, ar -> {
			return Result.createForValueData(ar);
		} ) ;

	}

	/**
	 * Execute an SQL statement without any returned data
	 * @param sql     	the sql statement to be executed
     * @param params   	an array of parameters which may be null or empty
	 * @param vertx     the Vertx object
     * @param handler   a handler to process the async result
	 */
	public static void exec(String sql, JsonArray params, Vertx vertx, Handler<Result> handler) {

		DB.execSQL(sql, params, vertx, handler, ar -> {
			return Result.createNoData(ar);
		} ) ;

	}
}
