package io.slingr.services.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows holding thread attributes.
 * <p>
 * User: smoyano
 * Date: 7/4/19
 */
public class ThreadAttributes {
    private static final ThreadLocal<Map<String, Object>> threadAttrs = ThreadLocal.withInitial(HashMap::new);

    public static Object get(String key) {
        return threadAttrs.get().get(key);
    }

    public static void set(String key, Object value) {
        threadAttrs.get().put(key, value);
    }

    public static void clear() {
        threadAttrs.get().clear();
    }

    public static void remove(String key) {
        threadAttrs.get().remove(key);
    }
}