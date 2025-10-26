package api.petstore;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

public final class RestClient {
    private final RequestSpecification baseSpec;

    public RestClient(String baseUrl) {
        this.baseSpec = new RequestSpecBuilder()
                .setBaseUri(Objects.requireNonNull(baseUrl))
                .setContentType("application/json")
                .build();
    }

    // ---- Generic, typed ----
    public <T> T get(String path, Class<T> type) {
        return get(path, null, 200, type);
    }
    public <T> T get(String path, Map<String, ?> query, int expected, Class<T> type) {
        var req = io.restassured.RestAssured.given().spec(baseSpec).log().all();
        if (query != null && !query.isEmpty()) req = req.queryParams(query);
        var resp = req.when().get(path);
        return resp.then().statusCode(expected).log().all().extract().as(type);
    }

    public <T> T post(String path, Object body, Class<T> type) {
        return post(path, body, 200, type);
    }
    public <T> T post(String path, Object body, int expected, Class<T> type) {
        var resp = io.restassured.RestAssured.given().spec(baseSpec).log().all()
                .body(body).when().post(path);
        return resp.then().statusCode(expected).log().all().extract().as(type);
    }

    public <T> T put(String path, Object body, Class<T> type) {
        return put(path, body, 200, type);
    }
    public <T> T put(String path, Object body, int expected, Class<T> type) {
        var resp = io.restassured.RestAssured.given().spec(baseSpec).log().all()
                .body(body).when().put(path);
        return resp.then().statusCode(expected).log().all().extract().as(type);
    }

    // ---- Raw + helpers ----
    public Response getRaw(String path) {
        var resp = io.restassured.RestAssured.given().spec(baseSpec).log().all().when().get(path);
        return resp.then().log().all().extract().response();
    }

    public Response deleteRaw(String path) {
        var resp = io.restassured.RestAssured.given().spec(baseSpec).log().all().when().delete(path);
        return resp.then().log().all().extract().response();
    }

    public boolean deleteOk(String path, int... okStatuses) {
        var sc = deleteRaw(path).statusCode();
        if (okStatuses == null || okStatuses.length == 0) okStatuses = new int[]{200, 204};
        int code = sc;
        return IntStream.of(okStatuses).anyMatch(s -> s == code);
    }
}