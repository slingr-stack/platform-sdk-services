package io.slingr.services.utils.converters;

import io.slingr.services.services.exchange.Parameter;
import io.slingr.services.utils.EmailUtils;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.Strings;
import io.slingr.services.utils.XmlUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Multipart;
import java.io.InputStream;
import java.util.*;

/**
 * Converts the inputs to Json.
 *
 * <p>Created by lefunes on 23/04/15.
 */
public final class JsonConverter {
    private static final Logger logger = LoggerFactory.getLogger(JsonConverter.class);

    public static Json fromObject(Object message) {
        try{
            return baseFromObject(message);
        } catch (Exception ex){
            ex.printStackTrace();
            return fromString("{ \"error\":\""+ ex +"\"}");
        }
    }

    public static Json baseFromObject(Object message){
        return baseFromObject(message, true);
    }

    public static Json baseFromObject(Object message, boolean showErrors){
        if(message == null){
            return Json.map();
        } else if (message instanceof Json) {
            return fromJson((Json) message);
        } else if (message instanceof JsonSource) {
            return fromJsonSource((JsonSource) message);
        } else if (message instanceof Map) {
            return fromMap((Map) message);
        } else if (message instanceof List) {
            return fromList((List) message);
        } else if (message instanceof InputStream) {
            return fromInputStream((InputStream) message);
        } else if (message instanceof Multipart) {
            return fromMultipart((Multipart) message);
        } else {
            return fromString(message.toString(), showErrors);
        }
    }

    public static Json fromJson(Json message) {
        if(message == null){
            return Json.map();
        }
        return message;
    }

    public static Json fromJsonSource(JsonSource message) {
        if(message == null){
            return Json.map();
        }
        return message.toJson();
    }

    public static Json fromMap(Map message) {
        if(message == null){
            return Json.map();
        }
        return Json.fromMap(message);
    }

    public static Json fromList(List message) {
        if(message == null){
            return Json.list();
        }
        return Json.fromList(message);
    }

    private static Json fromString(String message) {
        return fromString(message, true);
    }

    private static Json fromString(String message, boolean showErrors) {
        return Json.parse(message, showErrors);
    }

    /**
     * Returns the equivalent Json object or null.
     *
     * @param is input stream to convert
     * @return Json object or null
     */
    public static Json fromInputStream(InputStream is) {
        final String value = Strings.readAsString(is);
        if(value != null) {
            return convertString(value);
        }
        return null;
    }

    public static Json fromMultipart(Multipart multipart) {
        return EmailUtils.convertMultipart(multipart);
    }

    /**
     * Returns the equivalent Json object or null.
     *
     * @param value value to convert
     * @return Json object or null
     */
    public static Json convertString(String value) {
        return convertString(value, (String) null);
    }

    /**
     * Returns the equivalent Json object or null. The headers are used to find the content type, if this is found, we
     * use the default converter for that type.
     *
     * @param value value to convert
     * @param headers headers of request. these are used to find the content type of the value
     * @return Json object or null
     */
    public static Json convertString(String value, Json headers) {
        String contentType = null;
        if(headers != null){
            contentType = headers.string(Parameter.CONTENT_TYPE);
        }
        return convertString(value, contentType);
    }

    /**
     * Returns the equivalent Json object or null. The headers are used to find the content type, if this is found, we
     * use the default converter for that type.
     *
     * @param value value to convert
     * @param headers headers of request. these are used to find the content type of the value
     * @return Json object or null
     */
    public static Json convertString(String value, Map<String, Object> headers) {
        String contentType = null;
        if(headers != null){
            final Object contentTypeObject = headers.get(Parameter.CONTENT_TYPE);
            if (contentTypeObject instanceof String) {
                contentType = (String) contentTypeObject;
            }
        }
        return convertString(value, contentType);
    }

    /**
     * Returns the equivalent Json object or null. If content type is provided, we use the default converter for that
     * type.
     *
     * @param value value to convert
     * @param contentType content type of the value
     * @return Json object or null
     */
    public static Json convertString(String value, String contentType) {
        return convertString(value, contentType, true);
    }

    /**
     * Returns the equivalent Json object or null. If content type is provided, we use the default converter for that
     * type.
     *
     * @param value value to convert
     * @param contentType content type of the value
     * @param showErrors true if want to show warnings on console
     * @return Json object or null
     */
    public static Json convertString(String value, String contentType, boolean showErrors) {
        Json response = null;

        if (StringUtils.isNotBlank(contentType)) {
            if (ContentTypeFormat.isJsonContentType(contentType)) {
                // try to convert JSON string
                try {
                    response = Json.parse(value);
                } catch (Exception e) {
                    if(showErrors) {
                        logger.warn(String.format("Error parsing JSON from input stream [%s]", value));
                    }
                    return null;
                }
            } else if (ContentTypeFormat.isUrlEncodedFormContentType(contentType)) {
                // try to convert the URL encoded form
                try {
                    response = convertFormToJson(value);
                } catch (Exception e) {
                    if(showErrors) {
                        logger.warn(String.format("Error parsing JSON from URL encoded form [%s]", value));
                    }
                    return null;
                }
            } else if (ContentTypeFormat.isXmlContentType(contentType)) {
                // try to convert the XMl document
                try {
                    response = XmlUtils.xmlToJson(value);
                } catch (Exception e) {
                    if(showErrors) {
                        logger.warn(String.format("Error parsing JSON from XML document [%s]", value));
                    }
                    return null;
                }
            } else if (ContentTypeFormat.isPlainTextContentType(contentType)) {
                if (StringUtils.isNotEmpty(value) && (value.startsWith("{") || value.startsWith("["))) {
                    // seems like it is a JSON with a wrong content type so we will try to parse it
                    try {
                        response = Json.parse(value);
                    } catch (Exception e) {
                        // maybe we were wrong...
                        response = Json.map()
                                .set(Parameter.PARAMS_BODY, StringUtils.isNotEmpty(value) ? value : "");
                    }
                } else {
                    response = Json.map()
                            .set(Parameter.PARAMS_BODY, StringUtils.isNotEmpty(value) ? value : "");
                }
            } else if (ContentTypeFormat.isHtmlContentType(contentType)) {
                response = Json.map()
                        .set(Parameter.PARAMS_BODY, StringUtils.isNotEmpty(value) ? value : "");
            }
        }

        if(response == null){
            // generic case, try to convert the string to JSON
            try {
                response = Json.parse(value, showErrors);
            } catch (Exception ex) {
                try {
                    response = XmlUtils.xmlToJson(value);
                } catch (Exception e2) {
                    try{
                        response = convertFormToJson(value, showErrors);
                    } catch (Exception ex3) {
                        if(showErrors) {
                            logger.warn(String.format("Error parsing JSON from input stream [%s]", value));
                        }
                    }
                }
            }
        }

        return response;
    }

    /**
     * Returns the equivalent Json object or null.
     *
     * @param value value to convert
     * @return Json object or null
     */
    public static Json convertFormToJson(String value) {
        return convertFormToJson(value, true);
    }

    /**
     * Returns the equivalent Json object or null.
     *
     * @param value value to convert
     * @param showErrors true if want to show warnings on console
     * @return Json object or null
     */
    public static Json convertFormToJson(final String value, boolean showErrors) {
        if(StringUtils.isBlank(value)){
            return Json.map();
        }
        final Json response = Json.map();
        final Json parametersError = Json.list();

        final String[] parameters = value.split("&");
        for (String parameter : parameters) {
            try {
                final String[] parts = Strings.urlDecode(parameter).split("=", 2);
                if (parts.length > 1) {
                    final Object val = parse(parts[1]);
                    if (parts[0].contains("[")) {
                        // the key contains an array, like: customer[id]=ASD123
                        final String[] ks = parts[0].split("[\\[\\]]+");

                        final ArrayList<String> keys = new ArrayList<>(Arrays.asList(ks));

                        setInternalValue(response, keys, val);
                    } else {
                        response.set(parts[0], val);
                    }
                }
            } catch (Exception ex){
                if(showErrors) {
                    logger.warn(String.format("Exception when try to parse parameter from form: %s", ex.getMessage()), ex);
                }
                parametersError.push(parameter);
            }
        }
        response.setIfNotEmpty("nonParsableParameters", parametersError);
        return response;
    }

    private static Json setInternalValue(Json parent, List<String> keys, Object value){
        if(parent == null || keys == null || keys.isEmpty() || value == null){
            return parent;
        }
        if(keys.size() == 1){
            parent.set(keys.get(0), value);
        } else {
            final String key = keys.remove(0);
            Json current = parent.json(key);
            if(current == null){
                current = Json.map();
            }
            current = setInternalValue(current, keys, value);

            parent.setIfNotNull(key, current);
        }
        return parent;
    }

    /**
     * Returns the equivalent Json object or the original object.
     *
     * @param value object to use to generate the json
     * @return the equivalent json or the original object.
     */
    private static Object parse(String value) {
        if (value != null) {
            try {
                Json response = Json.fromObject(value, true, true);
                return Objects.requireNonNullElse(response, value);
            } catch (Exception ex) {
                // do nothing
            }
        }
        return value;
    }
}
