package io.slingr.services.ws.exchange;

import io.slingr.services.utils.Json;
import io.slingr.services.utils.converters.JsonSource;

import java.io.InputStream;

/**
 * File included on an upload request
 *
 * <p>Created by lefunes on 23/04/18.
 */
public record UploadedFile(String name, String filename, String contentType, Long length, InputStream file, Json headers) implements JsonSource {

    /**
     * Creates an upload file
     *
     * @param name        name of the file
     * @param filename    received name of the file
     * @param contentType content type of the file
     * @param length      length of the file
     * @param file        input stream
     * @param headers     HTTP headers
     */
    public UploadedFile(String name, String filename, String contentType, Long length, InputStream file, Json headers) {
        this.name = name;
        this.filename = filename;
        this.contentType = contentType;
        this.length = length;
        this.file = file;
        this.headers = headers != null ? headers : Json.map();
    }

    /**
     * Returns the name of the file
     *
     * @return name of the file
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * Returns the received name of the file
     *
     * @return received name of the file
     */
    @Override
    public String filename() {
        return filename;
    }

    /**
     * Returns the content type of the file
     *
     * @return content type of the file
     */
    @Override
    public String contentType() {
        return contentType;
    }

    /**
     * Returns the length of the file
     *
     * @return length of the file
     */
    @Override
    public Long length() {
        return length;
    }

    /**
     * Input stream of the resource downloaded
     *
     * @return input stream
     */
    @Override
    public InputStream file() {
        return file;
    }

    /**
     * Returns the HTTP headers of the response
     *
     * @return HTTP headers
     */
    @Override
    public Json headers() {
        return headers;
    }

    @Override
    public Json toJson() {
        return Json.map()
                .set("name", name())
                .setIfNotEmpty("filename", filename())
                .setIfNotEmpty("contentType", contentType())
                .setIfNotNull("length", length())
                .setIfNotEmpty("file", file() != null)
                .setIfNotEmpty("headers", headers());
    }
}