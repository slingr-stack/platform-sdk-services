package io.slingr.svcs.services.rest;

import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.utils.Json;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RestClientTest {


    private RestClient restClient;

    @Before
    public void setUp() {
        restClient = new RestClient("", new RestClientFactory()) {
            @Override
            protected Json executeHttpRequest(RestMethod method, HttpRequest request) throws SvcException {
                return super.executeHttpRequest(method, request);
            }
        };
    }

    @Test
    public void encodeUrlIsTrueByDefect() {

        Json json = Json.map().set("params", Json.map().set("foo1", "bar1 bar3"));

        restClient.setPath("https://postman-echo.com/get");

        HttpRequest request = HttpRequest.fromJson(RestMethod.GET, json);
        request.setEncodeUrl(true);

        Json response = restClient.executeHttpRequest(RestMethod.GET, request);

        assertEquals("https://postman-echo.com/get/?foo1=bar1%20bar3", response.object("url"));

    }

    @Test
    public void encodeUrlIsFalse() {

        Json json = Json.map().set("params", Json.map().set("foo1", "bar1 bar3"));

        restClient.setPath("https://postman-echo.com/get");

        HttpRequest request = HttpRequest.fromJson(RestMethod.GET, json);
        request.setEncodeUrl(false);

        Json response = restClient.executeHttpRequest(RestMethod.GET, request);

        assertEquals("https://postman-echo.com/get/?foo1=bar1+bar3", response.object("url"));

    }


    @Test
    public void useSSLIsTrue() {

        Json json = Json.map().set("params", Json.map().set("foo1", "bar1"));

        restClient.setAllowExternalUrl(true);

        HttpRequest request = HttpRequest.fromJson(RestMethod.GET, json);
        request.setPath("https://postman-echo.com/status/200");
        request.setUseSSL(true);

        Json response = restClient.executeHttpRequest(RestMethod.GET, request);

        assertEquals(HttpStatus.SC_OK, response.object("status"));

    }

    @Test
    public void useSSLIsFalse() {
        try {

            Json json = Json.map().set("params", Json.map().set("foo1", "bar1"));

            restClient.setAllowExternalUrl(true);

            HttpRequest request = HttpRequest.fromJson(RestMethod.GET, json);
            request.setPath("https://postman-echo.com/status/200");
            request.setUseSSL(false);


        } catch (SvcException e) {
            assertEquals("Error processing request [https protocol is not supported]", e.getMessage());
        }

    }

    @Test
    public void useSSLIsFalseAndNextRequestUseSSLIsTrueByDefault() {

        try {

            Json json = Json.map().set("params", Json.map().set("foo1", "bar1"));

            restClient.setAllowExternalUrl(true);

            HttpRequest request = HttpRequest.fromJson(RestMethod.GET, json);
            request.setPath("https://postman-echo.com/status/200");
            request.setUseSSL(false);

        } catch (SvcException e) {
            assertEquals("Error processing request [https protocol is not supported]", e.getMessage());
        }

        Json json = Json.map().set("params",Json.map().set("foo1", "bar1"));

        HttpRequest request = HttpRequest.fromJson(RestMethod.GET, json);
        request.setPath("https://postman-echo.com/status/200");
        request.setUseSSL(true);

        Json response = restClient.executeHttpRequest(RestMethod.GET, request);

        assertEquals(HttpStatus.SC_OK, response.object("status"));

    }


}