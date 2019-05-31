package msalter.crypto;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import scala.collection.JavaConverters;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.mauricio.async.db.postgresql.exceptions.GenericDatabaseException;
import com.github.mauricio.async.db.postgresql.messages.backend.ErrorMessage;

import io.vertx.core.AsyncResult;;

/**
 * <p>
 * Standardised object for returning result of a method execution (eg a DB operation) to client
 * </p>
 * @author      Mark Salter <a href="mailto:msalter@swissonline.ch">msalter@swissonline.ch</a>
 * @version     0.1
 */
public class Result {

	// flag of operation success
	public Boolean ok;
	
	// message, normally hosting error message
	public String message;
	
	// the data - null in event of failure
	public Object data;

	public Result(Boolean ok, String message, Object data) {
		this.ok = ok;
		this.message = message;
		this.data = data;
	}

	public Result(Object data) {
		ok = true;
		message = null;
		this.data = data;
	}

	public Result(Exception ex) {
		ok = false;
		message = ex.getMessage();
		data = null;
	}

	public Result(AsyncResult<SQLConnection> ar, Boolean dummy) {

		if (ar.succeeded()) {
			
			// Success!
			ok = true;
			message = null;
			data = null;

		} else {
			
			// Failure!
			ok = false;
			data = null;
			this.message = Result.getErrorMessage(ar);

		}		  

	}

	/**
	 * Create a Result for an operation which should return multiple rows
	 * 
	 * @param ar     the Vertx AsyncResult object
	 * @return		a Result object wrapping the outcome 
	 */
	public static Result createForRowsData(AsyncResult<ResultSet> ar) {
		
		Result result;
		
		if (ar.succeeded()) {
			
			// Success!
			ResultSet resultSet = ar.result();
			
			result = new Result(true, null, resultSet.getRows());

		} else {
			
			// Failure!
			result = new Result(false, Result.getErrorMessage(ar), null);
			
		}	
		
		return result;
	}
	
	/**
	 * Create a Result for an operation which should return a single row
	 * 
	 * @param ar     the Vertx AsyncResult object
	 * @return		a Result object wrapping the outcome 
	 */
	public static Result createForRowData(AsyncResult<ResultSet> ar) {
		
		Result result;
		
		if (ar.succeeded()) {
			
			// Success!
			ResultSet resultSet = ar.result();
			
            List<JsonObject> rows = resultSet.getRows();
            
            if (rows.size() != 0) {
      			result = new Result(true, null, rows.get(0));
            } else {
    			// no record found but NOT failure (?)
    			result = new Result(true, null, null);
            }

		} else {
			
			// Failure!
			result = new Result(false, Result.getErrorMessage(ar), null);
			
		}	
		
		return result;
		
	}
	
	/**
	 * Create a Result for an operation which should return a single value
	 * 
	 * @param ar     the Vertx AsyncResult object
	 * @return		a Result object wrapping the outcome 
	 */
	public static Result createForValueData(AsyncResult<ResultSet> ar) {
		
		Result result;
		
		if (ar.succeeded()) {
			
			// Success!
			ResultSet resultSet = ar.result();
			
            List<JsonObject> rows = resultSet.getRows();
            
            if (rows.size() != 0) {
            	
            	JsonObject row = rows.get(0);
            	Set<String> fieldNames = row.fieldNames();
            	String fieldName = fieldNames.iterator().next();
            	result = new Result(true, null, row.getValue(fieldName));   

            } else {
            	
    			// no record found but NOT failure (?)
    			result = new Result(true, null, null);
            }

		} else {
			
			// Failure!
			result = new Result(false, Result.getErrorMessage(ar), null);
			
		}	
		
		return result;
		
	}
	
	/**
	 * Create a Result for an operation which does not return any data
	 * 
	 * @param ar     the Vertx AsyncResult object
	 * @return		a Result object wrapping the outcome 
	 */
	public static Result createNoData(AsyncResult<ResultSet> ar) {
		
		Result result;
		
		if (ar.succeeded()) {
			
			// Success!
   			result = new Result(true, null, null);

		} else {
			
			// Failure!
			result = new Result(false, Result.getErrorMessage(ar), null);
			
		}	
		
		return result;
		
	}

	/**
	 * Get the human-readable error message from a Vertx result
	 * 
	 * @param ar     the Vertx AsyncResult object
	 * @return		the error message
	 */
	private static String getErrorMessage(AsyncResult ar) {
		
		String message;
		
		// convert scala to java map
		Map<Object, String> fields = JavaConverters.mapAsJavaMapConverter(((GenericDatabaseException) ar.cause()).errorMessage().fields()).asJava();

		if (fields.containsKey('M')) {
			
			//TODO : Internationalisation of all messages...
			message = fields.get('M');
		}
		else {
			// the longer version
			message = ar.cause().getMessage();      	    	   
		}
		return message;
	}
	
	/**
	 * Convert to json
	 * 
	 * @return		Result as json object
	 */
	public JsonObject toJson() {
		return new JsonObject()
				.put("ok", ok)
				.put("message", message)
				.put("data", data);
	}
	
	/**
	 * Was the operation successful?
	 * 
	 * @return		Boolean indicating success or failure of operation
	 */
	public Boolean isOk() {
		return ok;
	}

}
