package msalter.crypto;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

/**
 * <p>
 * The Crypto App Test Suite. Unfinished (only one test implemented) but illustrates how to test async services. 
 * </p>
 * @author      Mark Salter <a href="mailto:msalter@swissonline.ch">msalter@swissonline.ch</a>
 * @version     0.1
 */
@RunWith(VertxUnitRunner.class)
public class AppTest {

	private Vertx vertx;

	@Before
	public void setUp(TestContext context) {

		// TODO : init all data

		// launch the services
		vertx = Vertx.vertx();

		vertx.deployVerticle(AccountService.class.getName(),
				context.asyncAssertSuccess());
		vertx.deployVerticle(OrderService.class.getName(),
				context.asyncAssertSuccess());
		vertx.deployVerticle(GatewayService.class.getName(),
				context.asyncAssertSuccess());
	}

	@After
	public void tearDown(TestContext context) {
		// TODO : delete all data
		vertx.close(context.asyncAssertSuccess());
	}

	@SuppressWarnings("deprecation")
	@Ignore
	@Test
	public void testFetchOrderDetails(TestContext context) {

		final Async async = context.async();

		System.out.println("testFetchOrderDetails");

		HttpClient client = vertx.createHttpClient();

		HttpClientRequest toReq = client.request(HttpMethod.GET, 8081, "127.0.0.1", "/orderdetails/1",   response -> {

			response.bodyHandler(body -> {		

				JsonObject data = body.toJsonObject().getJsonObject("data");

				Long order_id = data.getLong("id");
				Long expectedValue = (long) 1;

				context.assertEquals(expectedValue, order_id);

				async.complete();

			});
		});

		// send request
		toReq.end();

	}

	@Test
	public void dummy(TestContext context) {
		context.assertEquals(true,true);
	}


}
