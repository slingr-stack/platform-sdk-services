package io.slingr.services.services.rest;

import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.utils.Json;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("SpellCheckingInspection")
public class RestClientTest {
    private RestClient restClient;

    @Before
    public void setUp() {
        restClient = new RestClient("", new RestClientFactory()) {
            @Override
            protected Json executeHttpRequest(RestMethod method, HttpRequest request) throws ServiceException {
                return super.executeHttpRequest(method, request);
            }
        };

        restClient.setAllowExternalUrl(true);
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


            HttpRequest request = HttpRequest.fromJson(RestMethod.GET, json);
            request.setPath("https://postman-echo.com/status/200");
            request.setUseSSL(false);


        } catch (ServiceException e) {
            assertEquals("Error processing request [https protocol is not supported]", e.getMessage());
        }

    }

    @Test
    public void useSSLIsFalseAndNextRequestUseSSLIsTrueByDefault() {

        try {

            Json json = Json.map().set("params", Json.map().set("foo1", "bar1"));


            HttpRequest request = HttpRequest.fromJson(RestMethod.GET, json);
            request.setPath("https://postman-echo.com/status/200");
            request.setUseSSL(false);

        } catch (ServiceException e) {
            assertEquals("Error processing request [https protocol is not supported]", e.getMessage());
        }

        Json json = Json.map().set("params", Json.map().set("foo1", "bar1"));

        HttpRequest request = HttpRequest.fromJson(RestMethod.GET, json);
        request.setPath("https://postman-echo.com/status/200");
        request.setUseSSL(true);

        Json response = restClient.executeHttpRequest(RestMethod.GET, request);

        assertEquals(HttpStatus.SC_OK, response.object("status"));

    }

    @Test
    public void testDefaultGetRequestWithBasicAuthentication() {

        Json json = Json.map().set("authorization",
                Json.map().set("type", "basic").set("username", "postman").set("password", "password"));


        HttpRequest request = HttpRequest.fromJson(RestMethod.GET, json);
        request.setPath("https://postman-echo.com/basic-auth");
        request.setFullResponse(true);

        Json response = restClient.executeHttpRequest(RestMethod.GET, request);

        assertEquals(HttpStatus.SC_OK, response.object("status"));
    }

    @Test
    public void testDefaultGetRequestDigestAuthentication() {

        Json json = Json.map().set("authorization",
                Json.map().set("type", "digest").set("username", "postman").set("password", "password"));


        HttpRequest request = HttpRequest.fromJson(RestMethod.GET, json);
        request.setPath("https://postman-echo.com/digest-auth");
        request.setFullResponse(true);

        Json response = restClient.executeHttpRequest(RestMethod.GET, request);

        assertEquals(HttpStatus.SC_OK, response.object("status"));
    }

    @Test
    @Ignore("The consumerKey is deprecated")
    public void testDefaultGetRequestOauthAuthentication() {

            Json json = Json.map().set("authorization",
                Json.map().set("type", "oauth").set("consumerKey", "RKCGzna7bv9YD57c")
                        .set("signatureMethod", "HMAC-SHA1")
                        .set("consumerSecret", "D+EdQ-gs$-%@2Nu7"));


        HttpRequest request = HttpRequest.fromJson(RestMethod.GET, json);
        request.setPath("https://postman-echo.com/oauth1");
        request.setFullResponse(true);

        Json response = restClient.executeHttpRequest(RestMethod.GET, request);

        assertEquals(HttpStatus.SC_OK, response.object("status"));
    }

    @Test
    public void testConcurrentRequests()  {
        int concurrentThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentThreads);
        for (int i = 0; i < concurrentThreads; i++) {
            executorService.execute(() -> {
                int random = new Random().nextInt(2);
                if (random == 0) {
                    testDefaultGetRequestDigestAuthentication();
                } else {
                    testDefaultGetRequestWithBasicAuthentication();
                }
            });
        }
        executorService.shutdown();
    }
}