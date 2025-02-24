package io.slingr.services.utils;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;

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
            final String parentKey1 = parentKey.isEmpty() ? key : parentKey + "[" + key + "]";
            if (value instanceof Map) {
                flattenJson(parentKey1, (Map<String, Object>) value, form);
            } else if (value instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    String indexedKey = parentKey.isEmpty() ? key + "[" + i + "]" : parentKey + "[" + key + "][" + i + "]";
                    form.param(indexedKey, list.get(i).toString());
                }
            } else if (value != null){
                form.param(parentKey1, value.toString());
            }
        }
    }

    /**
     * Convert a JSON object to a FormDataMultiPart.
     *
     * @param content JSON object
     * @return FormDataMultiPart
     */
    public static FormDataMultiPart convertFromJsonToFormDataMultiPart(Json content) {
        FormDataMultiPart formDataMultiPart = new FormDataMultiPart();

        if (content != null && content.isMap()) {
            flattenJson("", content.toMap(), formDataMultiPart);
        }

        return formDataMultiPart;
    }

    private static void flattenJson(String parentKey, Map<String, Object> map, FormDataMultiPart formDataMultiPart) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            final String parentKey1 = parentKey.isEmpty() ? key : parentKey + "[" + key + "]";
            if (value instanceof Map) {
                flattenJson(parentKey1, (Map<String, Object>) value, formDataMultiPart);
            } else if (value instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    String indexedKey = parentKey.isEmpty() ? key + "[" + i + "]" : parentKey + "[" + key + "][" + i + "]";
                    formDataMultiPart.field(indexedKey, list.get(i).toString());
                }
            } else if (value != null) {
                formDataMultiPart.field(parentKey1, value.toString());
            }
        }
    }
}