package context;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;


import java.util.HashMap;
import java.util.Map;

/**
 * Shared context between step definition classes using Cucumber's PicoContainer.
 * Stores state relevant to the current scenario's request and response cycle.
 */
public class ScenarioContext {

    // Key-value storage for request payload fields (e.g., username, password for JSON/Form bodies)
    private final Map<String, Object> payload = new HashMap<>();

    // General-purpose key-value storage for scenario-scoped data (e.g., stored tokens, IDs)
    private final Map<String, String> data = new HashMap<>();

    // Request Content-Type header value
    private String contentType;

    // Optional raw body string for non-structured requests (e.g., malicious payloads)
    private String rawBody;

    // The response received from the last executed request
    private Response response;

    public void setPayload(String key, Object value) {
        payload.put(key, value);
    }

    public Map<String, Object> getPayload () {
        return payload;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentType(String defaultType) {
        return contentType == null ? defaultType : contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Response getResponse() {
        return response;
    }

    public JsonPath getJSONResponse() {
        return response.jsonPath();
    }

    public String getResponseString(String key) {
        return getJSONResponse().getString(key);
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public Map<String, String> getData() {
        return data;
    }

    public String getRawBody() {
        return rawBody;
    }

    public void setRawBody(String rawBody) {
        this.rawBody = rawBody;
    }
}
