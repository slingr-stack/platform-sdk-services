package io.slingr.services.services.rest.authentication;

import org.junit.Test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

public class AuthenticationTypeTest {

    @Test(expected = IllegalArgumentException.class)
    public void testFromTypeNullTypeThrowsException() {
        Map<String, String> params = new HashMap<>();
        params.put("type", null);

        AuthenticationType.fromType(params);

        fail("Expected IllegalArgumentException was not thrown.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromTypeNotExistTypeThrowsException() {
        Map<String, String> params = new HashMap<>();

        AuthenticationType.fromType(params);

        fail("Expected IllegalArgumentException was not thrown.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFromTypeTypeNotInEnumThrowsException() {
        Map<String, String> params = new HashMap<>();
        params.put("type", "invalidType");

        AuthenticationType.fromType(params);

        fail("Expected IllegalArgumentException was not thrown.");
    }

    @Test
    public void testFromTypeValidTypeReturnsAuthenticationType() {
        Map<String, String> params = new HashMap<>();
        params.put("type", "basic");

        AuthenticationType result = AuthenticationType.fromType(params);

        assertEquals(AuthenticationType.BASIC, result);
    }
}