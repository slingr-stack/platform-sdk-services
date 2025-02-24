package io.slingr.services.utils;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Form;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@SuppressWarnings("AssertBetweenInconvertibleTypes")
public class JsonTest {
    private static final Logger logger = LoggerFactory.getLogger(JsonTest.class);

    @Test
    public void testConstructors() {
        Json json;

        // empty map
        json = Json.map();
        assertNotNull(json);
        assertTrue(json.isMap());
        assertFalse(json.isList());
        assertTrue(json.isEmpty());
        assertFalse(json.isNotEmpty());
        assertEquals(0, json.size());

        // empty list
        json = Json.list();
        assertNotNull(json);
        assertFalse(json.isMap());
        assertTrue(json.isList());
        assertTrue(json.isEmpty());
        assertFalse(json.isNotEmpty());
        assertEquals(0, json.size());

        // empty map
        json = Json.map(null, null);
        assertNotNull(json);
        assertTrue(json.isMap());
        assertFalse(json.isList());
        assertTrue(json.isEmpty());
        assertFalse(json.isNotEmpty());
        assertEquals(0, json.size());

        // empty list
        json = Json.list(null, null);
        assertNotNull(json);
        assertFalse(json.isMap());
        assertTrue(json.isList());
        assertTrue(json.isEmpty());
        assertFalse(json.isNotEmpty());
        assertEquals(0, json.size());

        // empty map
        json = Json.map("String", null);
        assertNotNull(json);
        assertTrue(json.isMap());
        assertFalse(json.isList());
        assertTrue(json.isEmpty());
        assertFalse(json.isNotEmpty());
        assertEquals(0, json.size());

        // empty list
        json = Json.list(Arrays.asList("original", "STRING", "string"), null);
        assertNotNull(json);
        assertFalse(json.isMap());
        assertTrue(json.isList());
        assertTrue(json.isEmpty());
        assertFalse(json.isNotEmpty());
        assertEquals(0, json.size());

        // empty map
        json = Json.map(null, (obj, json1) -> json1
                .set("original", obj.toString())
                .set("uppercase", obj.toString().toUpperCase())
                .set("lowercase", obj.toString().toLowerCase())
        );
        assertNotNull(json);
        assertTrue(json.isMap());
        assertFalse(json.isList());
        assertTrue(json.isEmpty());
        assertFalse(json.isNotEmpty());
        assertEquals(0, json.size());

        // empty list
        json = Json.list(null, String::toString);
        assertNotNull(json);
        assertFalse(json.isMap());
        assertTrue(json.isList());
        assertTrue(json.isEmpty());
        assertFalse(json.isNotEmpty());
        assertEquals(0, json.size());

        // map
        json = Json.map()
                .set("original", "String")
                .set("uppercase", "STRING")
                .set("lowercase", "string");
        assertNotNull(json);
        assertTrue(json.isMap());
        assertFalse(json.isList());
        assertFalse(json.isEmpty());
        assertTrue(json.isNotEmpty());
        assertEquals(3, json.size());
        assertEquals("String", json.string("original"));
        assertEquals("STRING", json.string("uppercase"));
        assertEquals("string", json.string("lowercase"));

        // list
        json = Json.list()
                .push("original")
                .push("STRING")
                .push("string");
        assertNotNull(json);
        assertFalse(json.isMap());
        assertTrue(json.isList());
        assertFalse(json.isEmpty());
        assertTrue(json.isNotEmpty());
        assertEquals(3, json.size());
        assertArrayEquals(new String[]{"original", "STRING", "string"}, json.toList().toArray(new Object[0]));

        // generated map
        json = Json.map("String", (obj, json1) -> json1
                .set("original", obj)
                .set("uppercase", obj.toUpperCase())
                .set("lowercase", obj.toLowerCase())
        );
        assertNotNull(json);
        assertTrue(json.isMap());
        assertFalse(json.isList());
        assertFalse(json.isEmpty());
        assertTrue(json.isNotEmpty());
        assertEquals(3, json.size());
        assertEquals("String", json.string("original"));
        assertEquals("STRING", json.string("uppercase"));
        assertEquals("string", json.string("lowercase"));

        // generated list
        json = Json.list(Arrays.asList("String", "STRING", "string"), String::toString);
        assertNotNull(json);
        assertFalse(json.isMap());
        assertTrue(json.isList());
        assertFalse(json.isEmpty());
        assertTrue(json.isNotEmpty());
        assertEquals(3, json.size());
        assertArrayEquals(new String[]{"String", "STRING", "string"}, json.toList().toArray(new Object[0]));

        // from map
        json = Json.fromMap(
                Json.map()
                        .set("original", "String")
                        .set("uppercase", "STRING")
                        .set("lowercase", "string")
                        .toMap()
        );
        assertNotNull(json);
        assertTrue(json.isMap());
        assertFalse(json.isList());
        assertFalse(json.isEmpty());
        assertTrue(json.isNotEmpty());
        assertEquals(3, json.size());
        assertEquals("String", json.string("original"));
        assertEquals("STRING", json.string("uppercase"));
        assertEquals("string", json.string("lowercase"));

        // from the list
        json = Json.fromList(
                Json.list()
                        .push("String")
                        .push("STRING")
                        .push("string")
                        .toList()
        );
        assertNotNull(json);
        assertFalse(json.isMap());
        assertTrue(json.isList());
        assertFalse(json.isEmpty());
        assertTrue(json.isNotEmpty());
        assertEquals(3, json.size());
        assertArrayEquals(new String[]{"String", "STRING", "string"}, json.toList().toArray(new Object[0]));
    }

    @Test
    public void testEquals() {
        Assert.assertFalse(Json.map().equals(null));
        Assert.assertFalse(Json.map().equals("hello"));
        Assert.assertTrue(Json.list().equals("[]"));
        Assert.assertTrue(Json.map().equals("{}"));

        Assert.assertFalse(Json.list().equals(null));
        Assert.assertFalse(Json.list().equals("hello"));
        Assert.assertTrue(Json.map().equals("{}"));
        Assert.assertTrue(Json.list().equals("[]"));

        Assert.assertTrue(Json.map().set("hello", "world").equals(" {\"hello\":  \"world\"  }  "));
        Assert.assertTrue(Json.parse("{\"hello\":\"world\",\"abc\":\"123\"}").equals(" {  \"abc\"    :  \"123\" ,    \"hello\":  \"world\"  }  "));
        Assert.assertTrue(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":\"123\"}")).equals(" {  \"abc\"    : {  \"abc\"    :  \"123\" ,    \"hello\":  \"world\"  } ,    \"hello\":  \"world\"  }  "));

        Assert.assertTrue(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":\"123\"}")).equals("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"123\"}}"));
        Assert.assertFalse(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":\"123\"}")).equals("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"456\"}}"));

        Assert.assertTrue(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"123\"}}")).equals("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"123\"}}}"));
        Assert.assertFalse(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"123\"}}")).equals("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"456\"}}}"));

        assertEquals(Json.map().set("hello", "world"), " {\"hello\":  \"world\"  }  ");
        assertEquals(Json.parse("{\"hello\":\"world\",\"abc\":\"123\"}"), " {  \"abc\"    :  \"123\" ,    \"hello\":  \"world\"  }  ");
        assertEquals(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":\"123\"}")), " {  \"abc\"    : {  \"abc\"    :  \"123\" ,    \"hello\":  \"world\"  } ,    \"hello\":  \"world\"  }  ");

        assertEquals(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":\"123\"}")), "{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"123\"}}");
        Assert.assertNotSame(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":\"123\"}")), "{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"456\"}}");

        assertEquals(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"123\"}}")), "{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"123\"}}}");
        Assert.assertNotSame(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"123\"}}")), "{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"456\"}}}");

        assertEquals(Json.map().set("hello", "world"), Json.parse(" {\"hello\":  \"world\"  }  "));
        assertEquals(Json.parse("{\"hello\":\"world\",\"abc\":\"123\"}"), Json.parse(" {  \"abc\"    :  \"123\" ,    \"hello\":  \"world\"  }  "));
        assertEquals(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":\"123\"}")), Json.parse(" {  \"abc\"    : {  \"abc\"    :  \"123\" ,    \"hello\":  \"world\"  } ,    \"hello\":  \"world\"  }  "));

        assertEquals(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":\"123\"}")), Json.parse("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"123\"}}"));
        Assert.assertNotSame(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":\"123\"}")), Json.parse("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"456\"}}"));

        assertEquals(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"123\"}}")), Json.parse("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"123\"}}}"));
        Assert.assertNotSame(Json.map().set("hello", "world").set("abc", Json.parse("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"123\"}}")), Json.parse("{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":{\"hello\":\"world\",\"abc\":\"456\"}}}"));
    }

    @Test
    public void testConvertValue() {
        logger.info("Starting testConvertValue()...");

        Json json = Json.map()
                .set("name", "test 1")
                .set("field1", "val 1")
                .set("field2", true)
                .set("nested", Json.map()
                        .set("name", "name nested")
                        .set("field1", "A")
                        .set("field2", 10)
                ).set("list", Json.list()
                        .push(Json.map()
                                .set("name", "converted")
                                .set("anotherField", "val")
                        )
                        .push(Json.map()
                                .set("name", "converted 2")
                                .set("anotherField", "val")
                        )
                ).set("list2", Json.list().push("__convert__").push("do not convert").push("__convert__"));

        json.traverse(new Json.Visitor() {
            @Override
            public Object convertValue(String key, Object value, String path) {
                if (value instanceof String && ((String) value).startsWith("__")) {
                    return ((String) value).toUpperCase();
                } else if ("name".equals(key)) {
                    return (value.toString()).toUpperCase();
                }
                return value;
            }
        });

        assertEquals("TEST 1", json.string("name"));
        assertEquals("NAME NESTED", json.json("nested").string("name"));
        assertEquals("CONVERTED", json.jsons("list").get(0).string("name"));
        assertEquals("CONVERTED 2", json.jsons("list").get(1).string("name"));
        assertEquals("__CONVERT__", json.strings("list2").get(0));
        assertEquals("__CONVERT__", json.strings("list2").get(2));
    }

    @Test
    public void testEnrich() {
        Json json = Json.map()
                .set("name", "test")
                .set("user", "123")
                .set("nestedOne1", Json.map()
                        .set("name", "A")
                        .set("user", "123")
                )
                .set("nestedOne2", Json.map()
                                .set("field1", "A")
                                .set("field2", "B")
                )
                .set("nestedMany", Json.list()
                                .push(Json.map()
                                                .set("name", "1")
                                                .set("user", "123")
                                )
                                .push(Json.map()
                                                .set("field1", "A")
                                                .set("field2", "B")
                                )
                );

        json.traverse(new Json.Visitor() {
            @Override
            public void enrich(String path, Json json) {
                if (json.contains("user")) {
                    json.set("userName", "pepe");
                }
            }
        });

        assertEquals("pepe", json.string("userName"));
        assertEquals("pepe", json.json("nestedOne1").string("userName"));
        assertNull("pepe", json.json("nestedOne2").string("userName"));
        assertEquals("pepe", json.jsons("nestedMany").get(0).string("userName"));
        assertNull("pepe", json.jsons("nestedMany").get(1).string("userName"));
    }

    @Test
    public void testTraverse() {
        logger.info("Starting testTraverse()...");

        Json json = Json.map()
                .set("a", "A")
                .set("b", Json.map().set("c", "C"))
                .set("d", Json.list().push(Json.map().set("t", "T")).push("E"))
                .set("f", Json.map().set("g", Json.map().set("h", "H")))
                .set("i", Json.map().set("j", Json.map().set("k", Json.map().set("l", "L")
                        .set("m", Json.list().push("N").push(Json.map().set("o", Json.list().push("P").push(Json.map().set("q", "Q")))))))
                )
                .set("r", Json.list().push(Json.list().push(Json.list().push(Json.list().push(Json.list().push("S"))))));

        final Json.Visitor visitor = new Json.Visitor() {
            @Override
            public Object convertValue(String key, Object value, String path) {
                if(value instanceof String) {
                    return value.toString().toLowerCase();
                }
                return value;
            }

            @Override
            public void enrich(String path, Json json) {
                json.set("_p", path.toUpperCase());
            }
        };

        json.traverse(visitor);

        assertNull(json.string("_p"));
        assertEquals("a", json.string("a"));
        assertEquals("b", json.json("b").string("_p"));
        assertEquals("c", json.json("b").string("c"));
        assertEquals("d[0]", ((Map<?, ?>) json.objects("d").get(0)).get("_p"));
        assertEquals("t", ((Map<?, ?>) json.objects("d").get(0)).get("t"));
        assertEquals("e", json.objects("d").get(1));
        assertEquals("f", json.json("f").string("_p"));
        assertEquals("f.g", json.json("f").json("g").string("_p"));
        assertEquals("h", json.json("f").json("g").string("h"));
        assertEquals("i", json.json("i").string("_p"));
        assertEquals("i.j", json.json("i").json("j").string("_p"));
        assertEquals("i.j.k", json.json("i").json("j").json("k").string("_p"));
        assertEquals("l", json.json("i").json("j").json("k").string("l"));
        assertEquals("n", json.json("i").json("j").json("k").objects("m").get(0));
        assertEquals("i.j.k.m[1]", ((Map<?, ?>) json.json("i").json("j").json("k").objects("m").get(1)).get("_p"));
        assertEquals("p", ((List<?>) ((Map<?, ?>) json.json("i").json("j").json("k").objects("m").get(1)).get("o")).get(0));
        assertEquals("i.j.k.m[1].o[1]", ((Map<?, ?>)((List<?>) ((Map<?, ?>) json.json("i").json("j").json("k").objects("m").get(1)).get("o")).get(1)).get("_p"));
        assertEquals("q", ((Map<?, ?>) ((List<?>) ((Map<?, ?>) json.json("i").json("j").json("k").objects("m").get(1)).get("o")).get(1)).get("q"));
        assertEquals("s", ((List<?>)((List<?>)((List<?>)((List<?>) json.objects("r").get(0)).get(0)).get(0)).get(0)).get(0));
    }

    @Test
    public void testMerge() {
        Json mergeJson = Json.map();
        mergeJson.merge(Json.map().set("a", 123));
        assertEquals(Json.map().set("a", 123), mergeJson);

        mergeJson = Json.map().set("a", 123);
        mergeJson.merge(null);
        assertEquals(Json.map().set("a", 123), mergeJson);

        mergeJson = Json.map().set("a", 123);
        mergeJson.merge(Json.map());
        assertEquals(Json.map().set("a", 123), mergeJson);

        mergeJson = Json.map().set("a", 789);
        mergeJson.merge(Json.map().set("a", 123));
        assertEquals(Json.map().set("a", 123), mergeJson);

        mergeJson = Json.map().set("b", 789);
        mergeJson.merge(Json.map().set("a", 123));
        assertEquals(Json.map().set("a", 123).set("b", 789), mergeJson);

        mergeJson = Json.map().set("b", 345).set("c", 456);
        mergeJson.merge(Json.map().set("a", 123).set("b", 789));
        assertEquals(Json.map().set("a", 123).set("b", 789).set("c", 456), mergeJson);

        mergeJson = Json.map().set("a", Json.map().set("b", 123));
        mergeJson.merge(Json.map().set("a", Json.map().set("c", 456)));
        assertEquals(Json.map().set("a", Json.map().set("b", 123).set("c", 456)), mergeJson);
    }

    @Test
    public void testForms() {
        Form form = new Form();
        form.param("key", "value");
        Json test = Json.map().set("body", form);

        assertEquals(Json.map().set("body", Json.map().set("key", Json.list().push("value"))).toString(), test.toString());

        form = new Form("key2", "value2");
        test = Json.map().set("body", form);

        assertEquals(Json.map().set("body", Json.map().set("key2", Json.list().push("value2"))).toString(), test.toString());
    }

    @Test
    public void testLists() {
        assertEquals(Json.parse("[\"hola\", \"chau\"]"), Json.list().push("hola").push("chau"));
        assertNotEquals(Json.parse("[\"chau\", \"hola\"]"), Json.list().push("hola").push("chau"));
        assertNotEquals(Json.parse("[\"chau\", \"hola\"]"), Json.parse("[\"hola\", \"chau\"]"));
    }
}