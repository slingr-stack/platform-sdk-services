package io.slingr.services.utils;

import org.junit.Test;

import javax.ws.rs.core.Form;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class FormUtilsTest {
    @Test
    public void testArrayConversion() {
        Json json = Json.map()
                .set("prop1", "a")
                .set("prop2", "b")
                .set("array", Json.list()
                        .push("1")
                        .push("2"))
                .set("address", Json.map()
                        .set("city", "NY")
                        .set("state", null));
        Form form = FormUtils.convertFromJsonToForm(json);
        Map<String, List<String>> map = form.asMap();
        assertEquals("a", map.get("prop1").get(0));
        assertEquals("b", map.get("prop2").get(0));
        assertEquals("1", map.get("array[0]").get(0));
        assertEquals("2", map.get("array[1]").get(0));
    }
}
