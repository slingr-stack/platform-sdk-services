package io.slingr.services.utils;

import javax.ws.rs.core.Form;
import java.util.List;

/**
 * Utilities to deal with Forms.
 * <p>
 * Created by dgaviola on 22/08/17.
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
            content.forEachMap((key, value) -> {
                if (value instanceof List) {
                    for (Object item : ((List<?>) value)) {
                        form.param(key, item.toString());
                    }
                } else if (value instanceof Json && ((Json) value).isList()) {
                    for (Object item : ((Json) value).objects()) {
                        form.param(key, item.toString());
                    }
                } else if (value != null) {
                    form.param(key, value.toString());
                }
            });
        }
        return form;
    }
}
