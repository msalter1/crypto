package msalter.crypto;

import io.vertx.core.Vertx;

/**
 * <p>
 * The Crypto App. Launches 2 microservices, one for Account-related actions, the other for Order-related actions. A third service is launched 
 * as a reverse proxy Gateway to the other two. 
 * </p>
 * @author      Mark Salter <a href="mailto:msalter@swissonline.ch">msalter@swissonline.ch</a>
 * @version     0.1
 */
public class App 
{
 
    public static void main( String[] args )
    {
        System.out.println( "Starting...!" );        
	
        Vertx vertx;
        vertx = Vertx.vertx();
        vertx.deployVerticle(AccountService.class.getName()); // account-related service on 8082
        vertx.deployVerticle(OrderService.class.getName()); // order-related service on 8083
        vertx.deployVerticle(GatewayService.class.getName()); // api gateway service on 8081

        System.out.println( "Started!" );

    }
}
