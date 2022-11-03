package io.slingr.svcs.services;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by dgaviola on 18/07/16.
 */
public class HttpServiceTest {
    @Test
    public void testFileNameCalculation() {
        assertEquals("test.zip", HttpService.extractFileName("https://static.slingrs.io/test/test.zip"));
        assertEquals("icon.png", HttpService.extractFileName("https://test1.slingrs.io/dev/runtime/api/files/images/icon.png?forceDownload=true"));
        assertEquals("image.jpg", HttpService.extractFileName("image.jpg"));
        assertEquals("image.jpg", HttpService.extractFileName("/image.jpg"));
        assertEquals("image.jpg", HttpService.extractFileName("image.jpg?param=abc"));
        assertEquals("image.jpg", HttpService.extractFileName("/image.jpg?param=abc"));
    }
}
