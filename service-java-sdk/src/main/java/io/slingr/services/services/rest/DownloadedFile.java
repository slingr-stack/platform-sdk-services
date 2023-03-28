package io.slingr.services.services.rest;

import io.slingr.services.utils.Json;

import java.io.InputStream;
import java.util.Map;

/**
 * Response to a download request
 *
 * <p>Created by lefunes on 16/03/18.
 */
public class DownloadedFile {

    private final int status;
    private final InputStream file;
    private final Json headers;

    /**
     * Creates a download response
     *
     * @param status HTTP status
     * @param file input stream
     * @param headers HTTP headers
     */
    public DownloadedFile(int status, InputStream file, Json headers) {
        this.status = status;
        this.file = file;
        this.headers = headers;
    }

    /**
     * Creates a download response
     *
     * @param status HTTP status
     * @param file input stream
     * @param headers HTTP headers
     */
    public DownloadedFile(int status, InputStream file, Map headers) {
        this(status, file, headers != null ? Json.fromMap(headers) : Json.map());
    }

    /**
     * Returns the HTTP status of the response
     *
     * @return HTTP status
     */
    public int getStatus() {
        return status;
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
}
