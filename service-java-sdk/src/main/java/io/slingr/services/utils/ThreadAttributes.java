package io.slingr.services.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows to hold thread attributes.
 *
 * User: smoyano
 * Date: 7/4/19
 */
public class ThreadAttributes {
    private static ThreadLocal<Map<String, Object>> threadAttrs = new ThreadLocal<Map<String, Object>>() {
        @Override
        protected Map<String, Object> initialValue() {
            return new HashMap<>();
        }
    };

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
