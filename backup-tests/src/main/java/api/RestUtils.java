package api;

import io.restassured.response.Response;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.SpecificationQuerier;
import utils.AerospikeLogger;

public class RestUtils {

	public static RequestSpecification printRequest(RequestSpecification requestSpec) {
		QueryableRequestSpecification queryable = SpecificationQuerier.query(requestSpec);
		AerospikeLogger.info("The request url is: " + queryable.getURI());
		//AerospikeLogger.info("Headers: " + queryable.getHeaders().toString());
		if (queryable.getBody() != null) {
			AerospikeLogger.info("Body: " + queryable.getBody().toString());
		}
		return requestSpec;
	}

	public static void printResponse(Response response, String actionName) {
		if (!response.body().asPrettyString().equals(""))
			AerospikeLogger.info("The response body for " + actionName + " is: " + response.body().asPrettyString());
		AerospikeLogger.info("The status code for " + actionName + " is: " + response.getStatusCode());
	}
}
