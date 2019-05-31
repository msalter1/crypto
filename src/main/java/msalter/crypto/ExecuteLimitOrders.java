package msalter.crypto;

import java.util.List;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * <p>
 * Execute Limit Orders App. Retrieves current price from external service and executes all unprocessed limit orders where the current price is less than the limit price 
 * </p>
 * @author      Mark Salter <a href="mailto:msalter@swissonline.ch">msalter@swissonline.ch</a>
 * @version     0.1
 * 
 * TODO: Refactor as a scheduled service
 */
public class ExecuteLimitOrders {

	public static void main(String[] args) {

		System.out.println( "ExecuteLimitOrders" );

		executeLimitOrders();
	}

	/**
	 * Execute limit orders where current price is less than limit price
	 * 
	 * @param fut		the Vertx future object
	 * @param router    a router containing the routes to be served
	 * @param port     	the port
	 */
	@SuppressWarnings("deprecation")
	private static void executeLimitOrders() {

		System.out.println( "executeLimitOrders");

		//
		// get the current price from the external service
		//
		
		String path = "/btc-price";
		int port = 5000;

		Vertx vertx;
		vertx = Vertx.vertx();

		HttpClient client = vertx.createHttpClient();

		HttpClientRequest toReq = client
				.request(HttpMethod.GET, port, "127.0.0.1", path,   response -> { // (1)
					response.bodyHandler(body -> {
						if (response.statusCode() >= 500) { 
							//???
						} else {

							JsonObject data = body.toJsonObject();

							Double price = data.getDouble("price");
							
							//
							// get any unprocessed, uncancelled orders whre current price less than limit price
							//

							// set sql
							String sql = "SELECT * FROM client_order WHERE ? < limit_price AND status = 'created'";

							// set params
							JsonArray params = new JsonArray().add(price.toString());

							// execute and return rows representing the Order Details
							DB.execAndReturnRows(sql, params, vertx, result -> {

								if(result.ok) {

									List<JsonObject> rows = (List<JsonObject>)(result.data);

									if(rows != null && rows.size() != 0) {

										for (JsonObject row : rows) {

											// get order id
											long order_id = row.getLong("id");

											//
											// execute the order at this price, updating account balances
											//

											// set sql
											String executionSql = "SELECT * FROM execute_limit_order(?,?)";

											// set params
											JsonArray executionParams = new JsonArray().add(Long.toString(order_id)).add(price.toString());

											// execute and return rows representing the Order Details
											DB.execAndReturnRows(executionSql, executionParams, vertx, resultOfExecution -> {

												//System.out.println( "ExecuteLimitOrders - done! ");

											});


										}

									}
								}

							});

						}

					});
				});

		// send request
		toReq.end();

	}
}
