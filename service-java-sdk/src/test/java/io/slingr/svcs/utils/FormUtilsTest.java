package io.slingr.svcs.utils;

import org.junit.Test;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedMap;

import static org.junit.Assert.assertEquals;

public class FormUtilsTest {
    @Test
    public void testArrayConversion() {
        Json json = Json.map()
                .set("prop1", "a")
                .set("prop2", "b")
                .set("array", Json.list().push("1").push("2"));
        Form form = FormUtils.convertFromJsonToForm(json);
        MultivaluedMap<String, String> map = form.asMap();
        assertEquals("a", map.get("prop1").get(0));
        assertEquals("b", map.get("prop2").get(0));
        assertEquals("1", map.get("array").get(0));
        assertEquals("2", map.get("array").get(1));
    }
}
