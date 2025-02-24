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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * String utilities.
 */
public class Strings {
    private static final Logger logger = LoggerFactory.getLogger(Strings.class);

    /**
     * Static factory to retrieve a type 4 (pseudo randomly generated) UUID.
     * <p>
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
     * <p>
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
     * Parse an HTTP query parameters string. The keys without values are included on response.
     *
     * @param queryString string to parse
     * @return a json map with the parameters
     */
    public static Json parseQueryString(String queryString){
        return parseQueryString(queryString, true);
    }

    /**
     * Parse an HTTP query parameters string
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
    public static String urlEncode(String string, boolean throwExceptionOnError) {
        if(StringUtils.isNotBlank(string)) {
            try {
                return URLEncoder.encode(string, StandardCharsets.UTF_8).replace("+", "%20").replace("*", "%2A");
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
    public static String urlDecode(String string, boolean throwExceptionOnError) {
        if(StringUtils.isNotBlank(string)) {
            try {
                return URLDecoder.decode(string, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                if(throwExceptionOnError){
                    throw ex;
                } else {
                    logger.warn(String.format("Error when try to decode string: %s", ex.getMessage()));
                    logger.debug(String.format("String to decode: %s", string.length() < 500 ? string : string.substring(497)+"..."));
                }
            }
            return string;
        }
        return "";
    }
}