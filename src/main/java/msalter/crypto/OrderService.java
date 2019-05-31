package msalter.crypto;

import java.math.BigDecimal;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * <p>
 * Order service hosting all Order-related endpoints
 * </p>
 * @author      Mark Salter <a href="mailto:msalter@swissonline.ch">msalter@swissonline.ch</a>
 * @version     0.1
 */
public class OrderService extends BaseVerticle {

	/**
	 * Service endpoint - create a limit order
	 * 
	 * Params : 
	 * 
	 * 	account_id - the id of the account
	 * 	price_limit - the limit price
	 * 
	 * @param routingContext	the Vertx routing context
	 * 
	 * TODO : Add parameter validation
	 * TODO : Implement top level try catch exception handling for any non-sql exceptions
	 */
	private void createLimitOrder(RoutingContext routingContext) {

        System.out.println( "OrderService - createLimitOrder" );

        // get params
		final long account_id = new Long(routingContext.request().getParam("account_id"));
		final BigDecimal price_limit = new BigDecimal(routingContext.request().getParam("price_limit"));

        // define sql
		String query = "SELECT * FROM create_limit_order(?,?)";
		
		// set params
		JsonArray params = new JsonArray().add( Long.toString(account_id)).add(price_limit.toString());

		// execute and return single row representing the Order Details
		DB.execAndReturnRow(query, params, vertx, result -> {

	        // create the response
			setResponse(routingContext, result);

		});
	}

	/**
	 * Service endpoint - fetch order details
	 * 
	 * Params : 
	 * 
	 * 	order_id - the id of the order
	 * 
	 * @param routingContext	the Vertx routing context
	 * 
	 * TODO : Add parameter validation
	 * TODO : Implement top level try catch exception handling for any non-sql exceptions
	 */
	private void fetchOrderDetails(RoutingContext routingContext) {			
		
        System.out.println( "OrderService - fetchOrderDetails" );

        // get params
		final long order_id = new Long(routingContext.request().getParam("order_id"));

        // define sql
		String query = "SELECT * FROM fetch_order_details(?)";
		
		// set params
		JsonArray params = new JsonArray().add(Long.toString(order_id));

		// execute and return single row representing the Account Details
		DB.execAndReturnRow(query, params, vertx, result -> {

	        // create the response
			setResponse(routingContext, result);

		});				

	} 

	/**
	 * Start the server after defining the routes
	 * 
	 * @param fut		the Vertx future object
	 * 
	 * TODO : Outsource service routes, location and port to centralised service discovery point
	 * TODO : Implement exception handling
	 */
	@Override
	public void start(Future<Void> fut) { 

		// Create a router object.
		Router router = Router.router(vertx);

		router.route().handler(BodyHandler.create());
		
		router.post("/limitorder/").handler(this::createLimitOrder);
		router.get("/orderdetails/:order_id").handler(this::fetchOrderDetails); 	
		
		// start server
		startServer(fut, router, 8083);
		
        System.out.println( "OrderService - started" );
		
	}
}
