package io.slingr.services.services.rest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.slingr.services.utils.Json;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.conn.ConnectTimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketTimeoutException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

public class RestClientFactoryTest {


    private RestClientFactory restClientFactory;
    private Method method;
    private WireMockServer wireMockServer;


    @Before
    public void setUp() throws Exception {

        wireMockServer = new WireMockServer(8888);
        wireMockServer.start();


        WireMock.configureFor(wireMockServer.port());

        // Absolute URL
        stubFor(WireMock.get(urlEqualTo("/first-redirect"))
                .willReturn(aResponse()
                        .withStatus(301)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withHeader(HttpHeaders.LOCATION, "http://localhost:" + wireMockServer.port() + "/second-redirect")));

        // Relative URL
        stubFor(WireMock.get(urlEqualTo("/second-redirect"))
                .willReturn(aResponse()
                        .withStatus(302)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withHeader(HttpHeaders.LOCATION, "/final-redirect")));

        // Relative URL
        stubFor(WireMock.get(urlEqualTo("/final-redirect"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody("{ \"message\": \"Hello, World!\" }")));

        stubFor(WireMock.post(urlEqualTo("/redirectMethod"))
                .willReturn(aResponse()
                        .withStatus(301)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withHeader(HttpHeaders.LOCATION, "/final-redirect")));

        stubFor(WireMock.post(urlEqualTo("/final-redirect"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .withBody("{ \"message\": \"Hello, World! FROM POST\" }")));


        restClientFactory = new RestClientFactory();
        Class<? extends RestClientFactory> clazz = restClientFactory.getClass();

        method = clazz.getDeclaredMethod("request", RestMethod.class, WebTarget.class, Object.class, Json.class, HttpRequest.class);
        method.setAccessible(true);

    }


    @After
    public void teardown() {
        wireMockServer.stop();
    }

    @Test
    public void request_ok() throws Exception {

        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .build();

        Response response = (Response) method.invoke(restClientFactory,
                RestMethod.GET,
                restClientFactory.uri("https://postman-echo.com/get?foo1=bar1&foo2=bar2"),
                null, null, request);

        Json json = restClientFactory.processResponse(response, RestMethod.GET, true);

        assertEquals(HttpStatus.SC_OK, json.object("status"));


    }


    @Test
    public void requestPostWithBodyRawText() throws Exception {

        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .build();

        Json json = Json.map().set("Content-Type", "text/plain");

        Object content = "This is expected to be sent back as part of response body.";

        Response response = (Response) method.invoke(restClientFactory,
                RestMethod.POST,
                restClientFactory.uri("https://postman-echo.com/post"), content, json, request);

        json = restClientFactory.processResponse(response, RestMethod.GET, true);

        assertEquals(HttpStatus.SC_OK, json.object("status"));

    }

    @Test
    public void requestPostWithBodyRawTextAndFullResponseIsTrue() throws Exception {

        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFullResponse(true)
                .build();

        Json json = Json.map().set("Content-Type", "text/plain");

        Object content = "This is expected to be sent back as part of response body.";

        Response response = (Response) method.invoke(restClientFactory,
                RestMethod.POST,
                restClientFactory.uri("https://postman-echo.com/post"), content, json, request);

        json = restClientFactory.processResponse(response, RestMethod.GET, request.isFullResponse());

        assertEquals(HttpStatus.SC_OK, json.object("status"));

    }


    @Test
    public void requestPostWithBodyRawTextAndFullResponseIsFalse() throws Exception {

        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFullResponse(false)
                .build();

        Json json = Json.map().set("Content-Type", "text/plain");

        Object content = "This is expected to be sent back as part of response body.";

        Response response = (Response) method.invoke(restClientFactory,
                RestMethod.POST,
                restClientFactory.uri("https://postman-echo.com/post"), content, json, request);

        json = restClientFactory.processResponse(response, RestMethod.GET, request.isFullResponse());

        assertNull(json.object("status"));

    }

    @Test
    public void requestGetWithReadTimeOutExceeds() throws Exception {

        //One millisecond is configured as the response time expected, so if it exceeds that time, an exception is thrown.
        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setReadTimeout(1)
                .build();

        try {
            method.invoke(restClientFactory,
                    RestMethod.GET,
                    restClientFactory.uri("https://postman-echo.com/get"),
                    null, null, request);
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause().getCause().getCause() instanceof SocketTimeoutException);
        }

    }


    @Test
    public void requestGetWithConnectionTimeoutExceeds() throws Exception {

        //One millisecond is configured as the connection timeout, so if it exceeds that time, an exception is thrown.
        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setConnectionTimeout(1)
                .build();

        try {
            method.invoke(restClientFactory,
                    RestMethod.GET,
                    restClientFactory.uri("https://postman-echo.com/get"),
                    null, null, request);
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause().getCause().getCause() instanceof ConnectTimeoutException);
        }

    }


    @Test
    public void followRedirectsIsTrue() throws InvocationTargetException, IllegalAccessException {

        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFollowRedirects(true)
                .setMaxRedirects(5)
                .build();

        Response response = (Response) method.invoke(restClientFactory,
                RestMethod.GET,
                restClientFactory.uri("http://localhost:" + wireMockServer.port() + "/first-redirect"),
                null, null, request);

        // Assert that the response status code is 301
        assertEquals(HttpStatus.SC_OK, response.getStatus());

    }


    @Test
    public void maxRedirectsIsReached() throws InvocationTargetException, IllegalAccessException {


        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFollowRedirects(true)
                .setMaxRedirects(1)
                .build();

        try {

            method.invoke(restClientFactory,
                    RestMethod.GET,
                    restClientFactory.uri("http://localhost:" + wireMockServer.port() + "/first-redirect"),
                    null, null, request);

        } catch (InvocationTargetException e) {
            assertTrue(e.getCause().getCause().getCause() instanceof ConnectTimeoutException);
        }
    }


    @Test
    public void followAuthorizationHeaderIsFalse() throws IllegalAccessException, InvocationTargetException {


        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFollowAuthorizationHeader(false)
                .build();

        Json headers = Json.map().set("Authorization", "Bearer token");


        method.invoke(restClientFactory,
                RestMethod.GET,
                restClientFactory.uri("http://localhost:" + wireMockServer.port() + "/first-redirect"),
                null, headers, request);

        //Verify that the final redirection not receives the Authorization header.
        verify(getRequestedFor(urlEqualTo("/final-redirect"))
                .withoutHeader("Authorization"));
    }


    @Test
    public void followAuthorizationHeaderIsTrue() throws IllegalAccessException, InvocationTargetException {


        HttpRequest request = new HttpRequest.HttpRequestBuilder()
                .setFollowAuthorizationHeader(true)
                .build();

        Json headers = Json.map().set("Authorization", "Bearer token");


        method.invoke(restClientFactory,
                RestMethod.GET,
                restClientFactory.uri("http://localhost:" + wireMockServer.port() + "/first-redirect"),
                null, headers, request);


        //Verify that the final redirection receives the Authorization header.
        verify(getRequestedFor(urlEqualTo("/final-redirect"))
                .withHeader("Authorization", equalTo("Bearer token")));
    }

    @Test
    public void dontRemoveRefererHeaderOnRedirect() throws InvocationTargetException, IllegalAccessException {
        HttpRequest request = new HttpRequest.HttpRequestBuilder().setRemoveRefererHeaderOnRedirect(false).build();

        method.invoke(restClientFactory, RestMethod.GET,
                restClientFactory.uri("http://localhost:" + wireMockServer.port() + "/first-redirect"),
                null, null, request);

        verify(getRequestedFor(urlEqualTo("/final-redirect")).withHeader("Referer", equalTo("http://localhost:8888/second-redirect")));
    }

    @Test
    public void removeRefererHeaderOnRedirect() throws InvocationTargetException, IllegalAccessException {
        HttpRequest request = new HttpRequest.HttpRequestBuilder().setRemoveRefererHeaderOnRedirect(true).build();

        method.invoke(restClientFactory, RestMethod.GET,
                restClientFactory.uri("http://localhost:" + wireMockServer.port() + "/first-redirect"),
                null, null, request);

        verify(getRequestedFor(urlEqualTo("/final-redirect")).withoutHeader(HttpHeaders.REFERER));
    }

    @Test
    public void followOriginalHttpMethod() throws InvocationTargetException, IllegalAccessException {
        HttpRequest request = new HttpRequest.HttpRequestBuilder().setFollowOriginalHttpMethod(true).build();

        Response response = (Response) method.invoke(restClientFactory, RestMethod.POST,
                restClientFactory.uri("http://localhost:" + wireMockServer.port() + "/redirectMethod"),
                null, null, request);

        Json json = restClientFactory.processResponse(response, RestMethod.POST, false);

        assertEquals("Hello, World! FROM POST", json.object("message"));
    }

    @Test
    public void dontFollowOriginalHttpMethod() throws InvocationTargetException, IllegalAccessException {
        HttpRequest request = new HttpRequest.HttpRequestBuilder().setFollowOriginalHttpMethod(false).build();

        Response response = (Response) method.invoke(restClientFactory, RestMethod.POST,
                restClientFactory.uri("http://localhost:" + wireMockServer.port() + "/redirectMethod"),
                null, null, request);

        Json json = restClientFactory.processResponse(response, RestMethod.POST, false);

        assertEquals("Hello, World!", json.object("message"));
    }

}