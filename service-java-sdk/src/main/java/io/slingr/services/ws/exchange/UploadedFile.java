package io.slingr.services.ws.exchange;

import io.slingr.services.utils.Json;
import io.slingr.services.utils.converters.JsonSource;

import java.io.InputStream;

/**
 * File included on an upload request
 *
 * <p>Created by lefunes on 23/04/18.
 */
public class UploadedFile implements JsonSource {

    private final String name;
    private final String filename;
    private final String contentType;
    private final Long length;
    private final InputStream file;
    private final Json headers;

    /**
     * Creates an upload file
     *
     * @param name name of the file
     * @param filename received name of the file
     * @param contentType content type of the file
     * @param length length of the file
     * @param file input stream
     * @param headers HTTP headers
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
    public String getName() {
        return name;
    }

    /**
     * Returns the received name of the file
     *
     * @return received name of the file
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns the content type of the file
     *
     * @return content type of the file
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the length of the file
     *
     * @return length of the file
     */
    public Long getLength() {
        return length;
    }

    /**
     * Input stream of the resource downloaded
     *
     * @return input stream
     */
    public InputStream getFile() {
        return file;
    }

    /**
     * Returns the HTTP headers of the response
     *
     * @return HTTP headers
     */
    public Json getHeaders() {
        return headers;
    }

    @Override
    public Json toJson() {
        return Json.map()
                .set("name", getName())
                .setIfNotEmpty("filename", getFilename())
                .setIfNotEmpty("contentType", getContentType())
                .setIfNotNull("length", getLength())
                .setIfNotEmpty("file", getFile() != null)
                .setIfNotEmpty("headers", getHeaders());
    }
}
