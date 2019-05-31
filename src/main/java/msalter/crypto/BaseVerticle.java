package msalter.crypto;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * <p>
 * Wrapper of verticle adding helper methods
 * </p>
 * @author      Mark Salter <a href="mailto:msalter@swissonline.ch">msalter@swissonline.ch</a>
 * @version     0.1
 */
public class BaseVerticle extends AbstractVerticle {

	/**
	 * Set the HTTP response with a json body
	 * 
	 * @param routingContext	the Vertx routing context
	 * @param result     		a Result object wrapping the outcome
	 */
	public void setResponse(RoutingContext routingContext, Result result) {

		if (!result.isOk()) {
			// TODO : correctly set all status codes...
			routingContext.response().setStatusCode(400).end();
		} else {

			routingContext.response()
					            .putHeader("content-type", "application/json; charset=utf-8")
					            .end(result.toJson().encodePrettily());
		}

	}

	/**
	 * Start the server
	 * 
	 * @param fut		the Vertx future object
	 * @param router    a router containing the routes to be served
	 * @param port     	the port
	 */
	public void startServer(Future<Void> fut, Router router, int port) {

		vertx.createHttpServer()
		.requestHandler(router::accept)
		.listen(port, result -> {
			if (result.succeeded()) {
				fut.complete();
			} else {
				fut.fail(result.cause());
			}
		});
	}
}
