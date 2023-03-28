package io.slingr.services.ws.exchange;

import io.slingr.services.services.rest.RestMethod;
import io.slingr.services.utils.Json;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WebServiceRequestTest {
    @Test
    public void testHeadersAreLowerCase() {
        Json headers = Json.map()
                .set("key1", "val1")
                .set("Key2", "val2");
        WebServiceRequest request = new WebServiceRequest(RestMethod.POST, "test", "test", headers, null, null, null);
        assertEquals(request.getHeader("key1"), "val1");
        assertEquals(request.getHeader("Key1"), "val1");
        assertEquals(request.getHeader("key2"), "val2");
        assertEquals(request.getHeader("Key2"), "val2");
        request.setHeader("KEY3", "val3");
        assertEquals(request.getHeader("key3"), "val3");
        assertEquals(request.getHeader("KEY3"), "val3");
        // check we cannot change headers using getHeaders()
        request.getHeaders().set("key1", "modifiedValue");
        assertEquals(request.getHeader("key1"), "val1");
    }
}
