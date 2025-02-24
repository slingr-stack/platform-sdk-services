package io.slingr.services.services.rest;

import io.slingr.services.utils.Json;

import java.io.InputStream;

/**
 * Response to a download request
 *
 * <p>Created by lefunes on 16/03/18.
 */
public record DownloadedFile(int status, InputStream file, Json headers) {

    /**
     * Creates a download response
     *
     * @param status  HTTP status
     * @param file    input stream
     * @param headers HTTP headers
     */
    public DownloadedFile {
    }

    /**
     * Returns the HTTP status of the response
     *
     * @return HTTP status
     */
    @Override
    public int status() {
        return status;
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
}