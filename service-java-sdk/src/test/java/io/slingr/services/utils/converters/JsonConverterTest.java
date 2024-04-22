package io.slingr.services.utils.converters;

import io.slingr.services.utils.Json;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@SuppressWarnings("SpellCheckingInspection")
public class JsonConverterTest {
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(JsonConverterTest.class);

    @Test
    public void testConvertFormToJson() {
        Json response;

        response = JsonConverter.convertFormToJson("abc=123&def[ghi]=true&def[jkl]=789&def[mno][pqr]=stuv&def[mno][wx][y][z]=9876543210");
        assertNotNull(response);
        assertEquals(2, response.size());
        assertEquals("123", response.string("abc"));

        Json def = response.json("def");
        assertNotNull(def);
        assertEquals(3, def.size());
        assertEquals("true", def.string("ghi"));
        assertEquals("789", def.string("jkl"));
        assertEquals("stuv", def.json("mno").string("pqr"));
        assertEquals("9876543210", def.json("mno").json("wx").json("y").string("z"));

    }

}
