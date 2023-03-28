package io.slingr.services.utils.converters;

import io.slingr.services.utils.EmailUtils;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.Multipart;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Converts the inputs to String.
 *
 * <p>Created by lefunes on 23/04/15.
 */
public final class StringConverter {
    private static final Logger logger = LoggerFactory.getLogger(StringConverter.class);

    public static String fromJsonSource(JsonSource message) {
        if(message != null){
            return message.toJson().toString();
        }
        return "";
    }

    public static String fromJson(Json message) {
        if(message != null){
            return message.toString();
        }
        return "";
    }

    public static String fromMap(Map message) {
        if(message != null){
            return Json.fromMap(message).toString();
        }
        return "";
    }

    public static String fromList(List message) {
        if(message != null){
            return Json.fromList(message).toString();
        }
        return "";
    }

    public static String fromObject(Object message) {
        if(message == null){
            return "";
        }
        if (message instanceof Json) {
            return fromJson((Json) message);
        } else if (message instanceof JsonSource) {
            return fromJsonSource((JsonSource) message);
        } else if (message instanceof Map) {
            return fromMap((Map) message);
        } else if (message instanceof List) {
            return fromList((List) message);
        } else if (message instanceof InputStream) {
            return Strings.readAsString((InputStream) message);
        } else if (message instanceof Multipart) {
            return EmailUtils.convertMultipart((Multipart) message).toString();
        } else {
            return message.toString();
        }
    }
}
