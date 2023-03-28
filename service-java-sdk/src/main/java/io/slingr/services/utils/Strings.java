package io.slingr.services.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * String utilities.

 * Created by lefunes on 13/03/18.
 */
public class Strings {
    private static final Logger logger = LoggerFactory.getLogger(Strings.class);

    /**
     * Static factory to retrieve a type 4 (pseudo randomly generated) UUID.
     *
     * The {@code UUID} is generated using a cryptographically strong pseudo
     * random number generator.
     *
     * @return a randomly generated {@code UUID}
     */
    public static UUID randomUUID() {
        return UUID.randomUUID();
    }

    /**
     * Static factory to retrieve a type 4 (pseudo randomly generated) UUID.
     *
     * The {@code UUID} is generated using a cryptographically strong pseudo
     * random number generator.
     *
     * @return a randomly generated {@code UUID}
     */
    public static String randomUUIDString() {
        return randomUUID().toString();
    }

    /**
     * <p>Creates a random string whose length is the number of characters
     * specified.</p>
     *
     * <p>Characters will be chosen from the set of alphabetic
     * characters.</p>
     *
     * @param count  the length of random string to create
     * @return the random string
     */
    public static String randomAlphabetic(int count) {
        return RandomStringUtils.randomAlphabetic(count);
    }

    /**
     * <p>Creates a random string whose length is the number of characters
     * specified.</p>
     *
     * <p>Characters will be chosen from the set of alpha-numeric
     * characters.</p>
     *
     * @param count  the length of random string to create
     * @return the random string
     */
    public static String randomAlphanumeric(int count) {
        return RandomStringUtils.randomAlphanumeric(count);
    }

    /**
     * Reads the data from the input stream.
     *
     * @param inputStream data to read
     * @return content of input stream
     */
    public static byte[] readBytes(InputStream inputStream) {
        try{
            return IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            logger.warn("Error parsing JSON from input stream", e);
            return null;
        }
    }

    /**
     * Reads the data from the input stream.
     *
     * @param inputStream data to read
     * @return content of input stream
     */
    public static String readAsString(InputStream inputStream){
        return readAsString(inputStream, StandardCharsets.UTF_8);
    }

    /**
     * Reads the data from the input stream.
     *
     * @param inputStream data to read
     * @param encoding the encoding to use, null means platform default
     * @return content of input stream
     */
    public static String readAsString(InputStream inputStream, Charset encoding){
        try {
            return IOUtils.toString(inputStream, encoding);
        } catch (IOException e) {
            logger.warn("Error getting string from input stream", e);
            return null;
        }
    }

    /**
     * Returns the content of the string as an input stream
     *
     * @param string string to convert
     * @return input stream
     */
    public static InputStream readAsInputStream(String string){
        if(string == null){
            string = "";
        }
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Masks a string in order to show log a sensible string (like a token) partially
     *
     * @param string string to show
     * @return masked string
     */
    public static String maskToken(String string){
        if(string == null){
            return "-";
        }

        String maskedToken = StringUtils.repeat(".", string.length());
        if(maskedToken.length()>20){
            maskedToken = string.substring(0,4)+maskedToken.substring(8)+string.substring(string.length()-4);
        } else if(maskedToken.length()>10){
            maskedToken = string.substring(0,2)+maskedToken.substring(4)+string.substring(string.length()-2);
        }
        return maskedToken;
    }

    /**
     * Parse a HTTP query parameters string. The keys without values are included on response.
     *
     * @param queryString string to parse
     * @return a json map with the parameters
     */
    public static Json parseQueryString(String queryString){
        return parseQueryString(queryString, true);
    }

    /**
     * Parse a HTTP query parameters string
     *
     * @param queryString string to parse
     * @param includeKeyWithoutValues true if you want to include the keys that does not have a value associated
     * @return a json map with the parameters
     */
    public static Json parseQueryString(String queryString, boolean includeKeyWithoutValues){
        final Json parameters = Json.map();
        if(StringUtils.isNotBlank(queryString)) {
            final String[] pm = queryString.split("&");
            if (pm.length > 0) {
                String k;
                for (String p : pm) {
                    p = p.trim();
                    final int i = p.indexOf("=");
                    if (i > 0 && i + 1 < p.length()) {
                        k = p.substring(0, i);
                        if(StringUtils.isNotBlank(k)) {
                            parameters.set(k.trim(), p.substring(i + 1).trim());
                        }
                    } else if(includeKeyWithoutValues) {
                        if(StringUtils.isNotBlank(p)) {
                            parameters.set(p, "true");
                        }
                    }
                }
            }
        }
        return parameters;
    }

    /**
     * Converts a json with parameters tp a HTTP query string
     *
     * @param parameters a json map with the query string parameters to convert
     * @return query string
     */
    public static String convertToQueryString(Json parameters){
        return convertToQueryString(parameters, false);
    }

    /**
     * Converts a json with parameters tp a HTTP query string
     *
     * @param parameters a json map with the query string parameters to convert
     * @param includeKeyWithoutValues true if you want to include with a 'true' as value the keys that does not have a
     *                                value associated
     * @return query string
     */
    public static String convertToQueryString(Json parameters, boolean includeKeyWithoutValues){
        final List<String> response = new ArrayList<>();
        if(parameters != null) {
            if(parameters.isMap()){
                parameters.forEachMapString((key, value) -> {
                    if(StringUtils.isNotBlank(key)) {
                        if (StringUtils.isBlank(value)) {
                            if (includeKeyWithoutValues) {
                                response.add(String.format("%s", key.trim()));
                            } else {
                                response.add(String.format("%s=true", key.trim()));
                            }
                        } else {
                            response.add(String.format("%s=%s", key.trim(), value.trim()));
                        }
                    }
                });
            } else {
                parameters.forEachList(key -> {
                    if(key != null && StringUtils.isNotBlank(key.toString())) {
                        if (includeKeyWithoutValues) {
                            response.add(String.format("%s", key.toString().trim()));
                        } else {
                            response.add(String.format("%s=true", key.toString().trim()));
                        }
                    }
                });
            }
        }
        final String query = response.stream().reduce((s1, s2) -> String.format("%s&%s", s1, s2)).orElse("");
        return Strings.urlEncode(query);
    }

    /**
     * Parse a HTTP query parameters string and expand the maps.
     * Per example {@code key1[subKey1][subKey2]=1234&key1[subKey1][subKey3]=5678&key2=abc } will be returned as
     * {@code {"key1":{"subKey1":{"subKey2":"1234", "subKey3":"5678"}}, "key2":"abc"} }
     *
     * @param parametersString string to parse
     * @return a json map with the parameters
     */
    public static Json parseAndExpandParameters(String parametersString) {
        final Json response = Json.map();

        final Json parameters = parseQueryString(parametersString, false);
        parameters.forEachMapString((parameter, value) -> {
            final String key = parameter.replaceAll("\\[", ".").replaceAll("]", "");
            if(StringUtils.isNotBlank(key)) {
                final String keyParts[] = key.split("\\.");
                if (keyParts.length > 0) {
                    Json currentLevel = response;
                    for (int i = 0; i < keyParts.length; i++) {
                        final String currentKeyPart = keyParts[i];
                        if (i == keyParts.length - 1) {
                            // if last part, store in current level
                            currentLevel.set(currentKeyPart, value);
                        } else {
                            // select current level
                            if (currentLevel.object(currentKeyPart) == null) {
                                final Json newLevel = Json.map();
                                currentLevel.set(currentKeyPart, newLevel);
                                currentLevel = newLevel;
                            } else {
                                currentLevel = currentLevel.json(currentKeyPart);
                            }
                        }
                    }
                }
            }
        });
        return response;
    }


    /**
     * Encodes a list of parameters in order to be used on the URLs
     *
     * @param parameters list of parameters
     * @return list of encoded parameter
     */
    public static List<String> urlEncode(List<String> parameters){
        return parameters.stream()
                .map(Strings::urlEncode)
                .collect(Collectors.toList());
    }

    /**
     * Encodes an object in order to be used on the URLs
     *
     * @param object object to encode
     * @return encoded string
     */
    public static String urlEncode(Object object){
        try {
            return urlEncode(object, false);
        } catch (Exception ex) {
            logger.warn(String.format("Error when try to encode object [%s]: %s", object, ex.getMessage()));
        }
        return "";
    }

    /**
     * Encodes an object in order to be used on the URLs
     *
     * @param object object to encode
     * @return encoded string
     */
    public static String urlEncode(Object object, boolean throwExceptionOnError) throws Exception {
        if(object != null) {
            return urlEncode((String) object, throwExceptionOnError);
        }
        return "";
    }

    /**
     * Encodes a string in order to be used on the URLs
     *
     * @param string string to encode
     * @return encoded string
     */
    public static String urlEncode(String string){
        try {
            return urlEncode(string, false);
        } catch (Exception ex) {
            logger.warn(String.format("Error when try to encode string [%s]: %s", string, ex.getMessage()));
        }
        return "";
    }

    /**
     * Encodes a string in order to be used on the URLs
     *
     * @param string string to encode
     * @param throwExceptionOnError true if the method have to throw an exception if an error happened
     * @return encoded string
     */
    public static String urlEncode(String string, boolean throwExceptionOnError) throws Exception {
        if(StringUtils.isNotBlank(string)) {
            try {
                return URLEncoder.encode(string, "UTF-8").replace("+", "%20").replace("*", "%2A");
            } catch (Exception ex) {
                if(throwExceptionOnError){
                    throw ex;
                } else {
                    logger.warn(String.format("Error when try to encode string [%s]: %s", string, ex.getMessage()));
                }
            }
            return string;
        }
        return "";
    }

    /**
     * Decodes an object received used on URLs
     *
     * @param object object to decode
     * @return decoded string
     */
    public static String urlDecode(Object object){
        try {
            return urlDecode(object, false);
        } catch (Exception ex) {
            logger.warn(String.format("Error when try to decode object [%s]: %s", object, ex.getMessage()));
        }
        return "";
    }

    /**
     * Decodes an object received used on URLs
     *
     * @param object object to decode
     * @param throwExceptionOnError true if the method have to throw an exception if an error happened
     * @return decoded string
     */
    public static String urlDecode(Object object, boolean throwExceptionOnError) throws Exception {
        if(object != null) {
            return urlDecode((String) object, throwExceptionOnError);
        }
        return "";
    }

    /**
     * Decodes a string received used on URLs
     *
     * @param string string to decode
     * @return decoded string
     */
    public static String urlDecode(String string){
        try {
            return urlDecode(string, false);
        } catch (Exception ex) {
            logger.warn(String.format("Error when try to decode string: %s", ex.getMessage()));
            logger.debug(String.format("String to decode: %s", string != null ? (string.length() < 500 ? string : string.substring(497)+"...") : "-"));
        }
        return "";
    }

    /**
     * Decodes a string received used on URLs
     *
     * @param string string to decode
     * @param throwExceptionOnError true if the method have to throw an exception if an error happened
     * @return decoded string
     */
    public static String urlDecode(String string, boolean throwExceptionOnError) throws Exception {
        if(StringUtils.isNotBlank(string)) {
            try {
                return URLDecoder.decode(string, "UTF-8");
            } catch (Exception ex) {
                if(throwExceptionOnError){
                    throw ex;
                } else {
                    logger.warn(String.format("Error when try to decode string: %s", ex.getMessage()));
                    logger.debug(String.format("String to decode: %s", string != null ? (string.length() < 500 ? string : string.substring(497)+"...") : "-"));
                }
            }
            return string;
        }
        return "";
    }
}
