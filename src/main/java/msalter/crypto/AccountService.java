package msalter.crypto;

import java.math.BigDecimal;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * <p>
 * Account service hosting all Account-related endpoints
 * </p>
 * @author      Mark Salter <a href="mailto:msalter@swissonline.ch">msalter@swissonline.ch</a>
 * @version     0.1
 */
public class AccountService extends BaseVerticle {

	/**
	 * Service endpoint - create an account
	 * 
	 * Params : 
	 * 
	 * 	name - the name of the account to open
	 * 	usd_balance - the opening usd balance
	 * 
	 * @param routingContext	the Vertx routing context
	 * 
	 * TODO : Add parameter validation
	 * TODO : Implement top level try catch exception handling for any non-sql exceptions
	 */
	private void createAccount(RoutingContext routingContext) {

        System.out.println( "AccountService - createAccount" );

        // get params
		final String name = routingContext.request().getParam("name");
		final BigDecimal usd_balance = new BigDecimal(routingContext.request().getParam("usd_balance"));

        // define sql
		String sql = "SELECT * FROM create_account(?,?)";
		
		// set params
		JsonArray params = new JsonArray().add(name).add(usd_balance.toString());

		// execute and return single row representing the Account Details
		DB.execAndReturnRow(sql, params, vertx, result -> {

	        // create the response
			setResponse(routingContext, result);

		});
	}

	/**
	 * Service endpoint - fetch account details
	 * 
	 * Params : 
	 * 
	 * 	account_id - the name of the account to open
	 * 
	 * @param routingContext	the Vertx routing context
	 * 
	 * TODO : Add parameter validation
	 * TODO : Implement top level try catch exception handling for any non-sql exceptions
	 */
	private void fetchAccountDetails(RoutingContext routingContext) {			
		
        System.out.println( "AccountService - fetchAccountDetails" );

        // get params
		final long account_id = new Long(routingContext.request().getParam("account_id"));

        // define sql
		String sql = "SELECT * FROM fetch_account_details(?)";
		
		// set params
		JsonArray params = new JsonArray().add(Long.toString(account_id));

		// execute and return single row representing the Account Details
		DB.execAndReturnRow(sql, params, vertx, result -> {

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
		
		router.post("/account/").handler(this::createAccount);
		router.get("/accountdetails/:account_id").handler(this::fetchAccountDetails); 	
		
		// start server
		startServer(fut, router, 8082);
		
        System.out.println( "AccountService - started" );
		
	}
}
