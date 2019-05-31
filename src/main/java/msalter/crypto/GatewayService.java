package msalter.crypto;

import java.util.Arrays;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * <p>
 * Gateway service - a reverse proxy that allows clients to access all microservices over a single endpoint
 * </p>
 * @author      Mark Salter <a href="mailto:msalter@swissonline.ch">msalter@swissonline.ch</a>
 * @version     0.1
 */
public class GatewayService extends BaseVerticle {

	/**
	 * Gateway service endpoint - routes and dispatches all requests to appropriate microservice
	 * 
	 * @param routingContext	the Vertx routing context
	 * 
	 * TODO : Add path validation and centralised service discovery / routing service
	 */
	private void routeRequest(RoutingContext context) {

		System.out.println( "GatewayService called...");

		// get request path
		String path = context.request().uri();

		int port = 0;

		// get the port - crude - we obviously need a better service discovery process...!
		if(Arrays.stream(new String[]{"/account","/accountdetails/"}).parallel().anyMatch(path::contains)) {
			port = 8082;
		}
		else if (Arrays.stream(new String[]{"/limitorder","/orderdetails/"}).parallel().anyMatch(path::contains)) {
			port = 8083;			
		}		

		HttpClient client = vertx.createHttpClient();

		dispatch(context, port, path, client); 

	}	

	/**
	 * Dispatch the request
	 * 
	 * @param routingContext	the Vertx routing context
	 * @param port     			the port
	 * @param path				the endpoint path
	 * @param client			the Http client
	 * 
	 * TODO : Add centralised service discovery / routing service
	 */
	@SuppressWarnings("deprecation")
	private void dispatch(RoutingContext context, int port, String path, HttpClient client) {

		HttpClientRequest toReq = client
				.request(context.request().method(), port, "127.0.0.1", path,   response -> { 
						response.bodyHandler(body -> {

						if (response.statusCode() >= 500) { 
							//???
						} else {
							HttpServerResponse toRsp = context.response()
									.setStatusCode(response.statusCode());
							response.headers().forEach(header -> {
								toRsp.putHeader(header.getKey(), header.getValue());
							});
							// send response
							toRsp.end(body); 
						}

					});
				});
		// set headers
		context.request().headers().forEach(header -> { 
			toReq.putHeader(header.getKey(), header.getValue());
		});
		if (context.user() != null) {
			    toReq.putHeader("user-principal", context.user().principal().encode());
		}
		// send request
		if (context.getBody() == null) {
			toReq.end();
		} else {
			toReq.end(context.getBody());
		}
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

		router.route("/*").handler(this::routeRequest); 

		// start server
		startServer(fut, router, 8081);

		System.out.println("GatewayService - started");

	}
}
