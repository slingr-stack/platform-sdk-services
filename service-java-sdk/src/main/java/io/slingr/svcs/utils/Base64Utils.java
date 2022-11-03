package io.slingr.svcs.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Helper that permits to work with Base64 streams
 *
 * <p>Created by lefunes on 02/09/15.
 */
public class Base64Utils {

    /**
     * Encodes binary data using the base64 algorithm but does not chunk the output.
     *
     * @param inputStream data to encode
     * @return string containing Base64 characters in their UTF-8 representation.
     */
    public static String encode(InputStream inputStream) throws IOException {
        return encode(Strings.readBytes(inputStream));
    }

    /**
     * Encodes binary data using the base64 algorithm but does not chunk the output.
     *
     * @param string data to encode
     * @return string containing Base64 characters in their UTF-8 representation.
     */
    public static String encode(String string){
        return encode(StringUtils.isBlank(string) ? "".getBytes() : string.getBytes());
    }

    /**
     * Encodes binary data using the base64 algorithm but does not chunk the output.
     *
     * @param bytes binary data to encode
     * @return string containing Base64 characters in their UTF-8 representation.
     */
    public static String encode(byte[] bytes){
        return new String(Base64.encodeBase64(bytes));
    }

    /**
     * Decodes a Base64 String into octets
     *
     * @param inputStream Input stream containing Base64 data
     * @return Array containing decoded data.
     */
    public static String decode(InputStream inputStream) throws IOException {
        return decode(Strings.readBytes(inputStream));
    }

    /**
     * Decodes Base64 data into octets
     *
     * @param base64Data Byte array containing Base64 data
     * @return Array containing decoded data.
     */
    public static String decode(byte[] base64Data){
        return new String(decodeData(base64Data));
    }

    /**
     * Decodes a Base64 String into octets
     *
     * @param base64String String containing Base64 data
     * @return Array containing decoded data.
     */
    public static String decode(String base64String){
        return new String(decodeData(base64String));
    }

    /**
     * Decodes Base64 data into octets
     *
     * @param base64Data Byte array containing Base64 data
     * @return Array containing decoded data.
     */
    public static byte[] decodeData(byte[] base64Data){
        return Base64.decodeBase64(base64Data);
    }

    /**
     * Decodes a Base64 String into octets
     *
     * @param base64String String containing Base64 data
     * @return Array containing decoded data.
     */
    public static byte[] decodeData(String base64String){
        return Base64.decodeBase64(base64String);
    }

    /**
     * Encodes the basic authorization using the base64 algorithm.
     *
     * @param username user name
     * @param password password
     * @return encoded basic authorization
     */
    public static String encodeBasicAuthorization(String username, String password){
        return encode(String.format("%s:%s", StringUtils.isNotBlank(username) ? username.trim() : "", StringUtils.isNotBlank(password) ? password.trim() : ""));
    }
}
