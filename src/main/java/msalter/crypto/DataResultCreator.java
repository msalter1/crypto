package msalter.crypto;

import io.vertx.core.AsyncResult;
import io.vertx.ext.sql.ResultSet;

/**
 * <p>
 * Functional interface for lambda to return a Result object based on passed AsyncResult
 * </p>
 * @author      Mark Salter <a href="mailto:msalter@swissonline.ch">msalter@swissonline.ch</a>
 * @version     0.1
 */
public interface DataResultCreator {

	/**
	 * Create a Result for a DB operation
	 * 
	 * @param ar     the Vertx AsyncResult object which should hold the data
	 * @return		a Result object wrapping the outcome 
	 * 
	 */	
	public Result createResult(AsyncResult<ResultSet> ar);
}
