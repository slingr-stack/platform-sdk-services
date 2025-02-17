package io.slingr.services.services;

import io.slingr.services.utils.Json;
import io.slingr.services.utils.tests.ExtensionBrokerMock;
import io.slingr.services.ws.exchange.FunctionRequest;
import org.apache.http.HttpStatus;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;

public class HttpServiceTest {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(HttpServiceTest.class);

    private HttpService httpService;

    @Test
    public void testFileNameCalculation() {
        assertEquals("test.zip", HttpService.extractFileName("https://static.slingrs.io/test/test.zip"));
        assertEquals("icon.png", HttpService.extractFileName("https://test1.slingrs.io/dev/runtime/api/files/images/icon.png?forceDownload=true"));
        assertEquals("image.jpg", HttpService.extractFileName("image.jpg"));
        assertEquals("image.jpg", HttpService.extractFileName("/image.jpg"));
        assertEquals("image.jpg", HttpService.extractFileName("image.jpg?param=abc"));
        assertEquals("image.jpg", HttpService.extractFileName("/image.jpg?param=abc"));
    }


    @Test
    public void testDefaultGetRequest() {

        buildHttpService("https://postman-echo.com/status/" + HttpStatus.SC_OK);

        FunctionRequest functionRequest = new FunctionRequest(Json.map());
        Json response = httpService.defaultGetRequest(functionRequest);
        assertEquals(HttpStatus.SC_OK, response.object("status"));
    }

    @Test
    public void testDefaultPostRequest() {

        buildHttpService("https://postman-echo.com/post");

        Json json = Json.map()
                .set("params", Json.map().set("body", "This is a body"));

        FunctionRequest functionRequest = new FunctionRequest(json);
        Json response = httpService.defaultPostRequest(functionRequest);
        assertEquals("This is a body", response.object("data"));
    }


    @Test
    public void testDefaultPutRequest() {

        buildHttpService("https://postman-echo.com/put");

        Json json = Json.map()
                .set("params", Json.map().set("body", "This is a body"));

        FunctionRequest functionRequest = new FunctionRequest(json);
        Json response = httpService.defaultPutRequest(functionRequest);
        assertEquals("This is a body", response.object("data"));
    }

    @Test
    public void testDefaultGetRequestWithParams() {

        buildHttpService("https://postman-echo.com/get");

        Json json = Json.map().set("params", Json.map().set("params",
                Json.map().set("foo1", "bar1").set("foo2", "bar2")));

        FunctionRequest functionRequest = new FunctionRequest(json);
        Json response = httpService.defaultGetRequest(functionRequest);
        assertEquals("bar1", ((Json)response.object("args")).object("foo1"));
        assertEquals("bar2", ((Json)response.object("args")).object("foo2"));
    }

    @Test
    public void testDefaultGetRequestWithCallback() {

        buildHttpService("https://postman-echo.com/status/" + HttpStatus.SC_OK);

        Json json = Json.map().set("id", "5f40a9a2-dfb8-4d29-9bf5-2f8c8945fe17");

        FunctionRequest functionRequest = new FunctionRequest(json);
        Json response = httpService.defaultGetRequest(functionRequest);
        assertEquals(HttpStatus.SC_OK, response.object("status"));
    }


    private void buildHttpService(String apiUri) {
        Events events = new Events(new ExtensionBrokerMock(), false);
        this.httpService = new HttpService(
                apiUri,
                events,
                null,
                false
        );
    }


}
