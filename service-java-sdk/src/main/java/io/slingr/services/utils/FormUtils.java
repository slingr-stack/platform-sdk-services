package io.slingr.services.utils;

import javax.ws.rs.core.Form;
import java.util.List;
import java.util.Map;

/**
 * Utilities to deal with Forms.
 * <p>
 * Created by agreggio on 03/11/22.
 */
public class FormUtils {

    /**
     * Convert a json object to a form
     *
     * @param content json object
     * @return form
     */
    public static Form convertFromJsonToForm(Json content) {
        final Form form = new Form();
        if (content != null && content.isMap()) {
            flattenJson("", content.toMap(), form);
        }
        return form;
    }

    private static void flattenJson(String parentKey, Map<String, Object> map, Form form) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flattenJson(parentKey.isEmpty() ? key : parentKey + "[" + key + "]", (Map<String, Object>) value, form);
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    String indexedKey = parentKey.isEmpty() ? key + "[" + i + "]" : parentKey + "[" + key + "][" + i + "]";
                    form.param(indexedKey, list.get(i).toString());
                }
            } else {
                String finalKey = parentKey.isEmpty() ? key : parentKey + "[" + key + "]";
                form.param(finalKey, value.toString());
            }
        }
    }
}
