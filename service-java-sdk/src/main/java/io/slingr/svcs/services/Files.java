package io.slingr.svcs.services;

import io.slingr.svcs.Svc;
import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.exceptions.ErrorCode;
import io.slingr.svcs.services.exchange.Parameter;
import io.slingr.svcs.services.rest.DownloadedFile;
import io.slingr.svcs.utils.Base64Utils;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.utils.Strings;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Manages all messages related to files exchanged with the Extension Broker
 *
 * <p>Created by lefunes on 20/03/18.
 */
public class Files {
    private static final Logger logger = LoggerFactory.getLogger(Scripts.class);

    private final ExtensionBrokerApi api;
    private final boolean debug;

    /**
     * Constructor only can be called by Extension Broker class
     *
     * @param api extension broker implementation
     * @param debug true if the service shows information useful for debug
     */
    public Files(ExtensionBrokerApi api, boolean debug) {
        this.api = api;
        this.debug = debug;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Files
    ///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Uploads the given file to the platform
     *
     * <p>The Services uses this method to upload files to the application.
     *
     * <p>The file content must be sent on the request as file parameter of a multipart/form-data. As result, extension broker
     * receives a json that includes the assigned fileId.
     *
     * @param filename name of the file with extension
     * @param content content of the file
     * @param contentType content type to use when the file is uploaded
     * @param base64 true if the content parameter is a Base64 encoded string
     * @return json that includes the file identifier (fileId) of the file on the platform
     * @throws SvcException if there is an issue with the exchange
     */
    public Json upload(String filename, String content, String contentType, boolean base64) throws SvcException {
        ExtensionBroker.isNotBlank(content, "empty file content");

        try {
            final byte[] byteContent;
            if (base64) {
                content = content.replace(" ", "+");
                byteContent = Base64Utils.decodeData(content.getBytes());
            } else {
                byteContent = content.getBytes();
            }
            return upload(filename, new ByteArrayInputStream(byteContent), contentType);
        } catch (SvcException rex){
            throw rex;
        } catch (Exception ex){
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to upload file [%s]: %s", filename, ex.getMessage()), ex);
        }
    }

    /**
     * Uploads the given file to the platform.
     *
     * <p>The Services uses this method to upload files to the application.
     *
     * <p>The file content must be sent on the request as file parameter of a multipart/form-data. As result, extension broker
     * receives a json that includes the assigned fileId.
     *
     * @param filename name of the file with extension
     * @param content content of the file
     * @param contentType content type to use when the file is uploaded
     * @return json that includes the file identifier (fileId) of the file on the platform
     * @throws SvcException if there is an issue with the exchange
     */
    public Json upload(String filename, InputStream content, String contentType) throws SvcException {
        ExtensionBroker.isNotBlank(filename, "empty file name");
        ExtensionBroker.isNotNull(content, "empty file content");

        if(debug) {
            logger.info(String.format("%s uploading file [%s] to application.", Svc.DEBUG, filename));
        }
        try {
            final Json response = api.uploadFile(filename, content, contentType);
            if(debug) {
                logger.info(String.format("%s file [%s] uploaded to application - file id [%s]", Svc.DEBUG, filename, response.string(Parameter.FILE_ID)));
            }
            return response;
        } catch (SvcException rex){
            throw rex;
        } catch (Exception ex){
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to upload file [%s]: %s", filename, ex.getMessage()), ex);
        }
    }

    /**
     * Uploads the given file to the platform.
     *
     * <p>The Services uses this method to upload files to the application.
     *
     * <p>The file content must be sent on the request as file parameter of a multipart/form-data. As result, extension broker
     * receives a json that includes the assigned fileId.
     *
     * @param filename name of the file with extension
     * @param file file previously downloaded from a REST service
     * @return json that includes the file identifier (fileId) of the file on the platform
     * @throws SvcException if there is an issue with the exchange
     */
    public Json upload(String filename, DownloadedFile file) throws SvcException {
        ExtensionBroker.isNotBlank(filename, "empty file name");
        ExtensionBroker.isNotNull(file, "empty downloaded file");

        try {
            String contentType = "";
            if(file.getHeaders() != null) {
                final String ct = file.getHeaders().string(Parameter.CONTENT_TYPE);
                if(StringUtils.isNotBlank(ct)) {
                    contentType = ct;
                }
            }
            return upload(filename, file.getFile(), contentType);
        } catch (SvcException rex){
            throw rex;
        } catch (Exception ex){
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to upload file [%s]: %s", filename, ex.getMessage()), ex);
        }
    }

    /**
     * Downloads from the application a file using its file ID and returns its content
     *
     * <p>The service gets the content of a file stored on the application using this method. The file id value is used
     * to identify the file on app. As result, we obtain the file content as a string.
     *
     * @param fileId identifier of the file to download
     * @param base64 true if the content must be encoded as Base64 string
     * @return file content
     * @throws SvcException if there is an issue with the exchange
     */
    public String download(String fileId, boolean base64) throws SvcException {
        try {
            final DownloadedFile response = download(fileId);
            return extractContent(response, base64);
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.permanent(ErrorCode.CLIENT, String.format("Exception when try to download file [%s] - Exception [%s]", fileId, ex.getMessage()), ex);
        }
    }

    /**
     * Downloads from the application a file using its file ID.
     *
     * <p>The service gets the content of a file stored on the application using this method. The file id value is used
     * to identify the file on app. As result, we obtain the file content as a stream of bytes.
     *
     * @param fileId identifier of the file to download
     * @return file data that includes the stream of the file content
     * @throws SvcException if there is an issue with the exchange
     */
    public DownloadedFile download(String fileId) throws SvcException {
        ExtensionBroker.isNotBlank(fileId, "empty file id");

        if(debug) {
            logger.info(String.format("%s downloading file [%s] from application.", Svc.DEBUG, fileId));
        }
        try {
            final DownloadedFile response = api.downloadFile(fileId);
            if(response == null || response.getFile() == null){
                throw SvcException.permanent(ErrorCode.CLIENT, String.format("File [%s] not found on application.", fileId));
            }
            if(debug) {
                logger.info(String.format("%s file [%s] downloaded from application.", Svc.DEBUG, fileId));
            }
            return response;
        } catch (SvcException rex){
            throw rex;
        } catch (Exception ex){
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to download file [%s]: %s", fileId, ex.getMessage()), ex);
        }
    }

    /**
     * Extracts the file content from a received {@code DownloadedFile} object
     *
     * @param fileDownloaded received download response object
     * @param base64 true if the content must be encoded as Base64 string
     * @return file content
     * @throws SvcException if there is an issue with the exchange
     */
    public String extractContent(DownloadedFile fileDownloaded, boolean base64) throws SvcException {
        ExtensionBroker.isNotNull(fileDownloaded, "Invalid file");
        ExtensionBroker.isNotNull(fileDownloaded.getFile(), "Empty file content");

        try {
            if (base64) {
                return Base64Utils.encode(fileDownloaded.getFile());
            } else {
                return Strings.readAsString(fileDownloaded.getFile());
            }
        } catch (SvcException ex) {
            throw ex;
        } catch (Exception ex) {
            throw SvcException.permanent(ErrorCode.CLIENT, String.format("Exception when try to convert downloaded file - Exception [%s]", ex.getMessage()), ex);
        }
    }

    /**
     * Gets the file metadata using its ID from the application.
     *
     * <p>The service gets the metadata of a file stored on the application using this method. The file id value is
     * used to identify the file on app. As result, we receive a json that includes the file metadata.
     *
     * @param fileId identifier of the file
     * @return json that contains file metadata
     * @throws SvcException if there is an issue with the exchange
     */
    public Json metadata(String fileId) throws SvcException {
        ExtensionBroker.isNotBlank(fileId, "empty file id");

        if(debug) {
            logger.info(String.format("%s getting file [%s] metadata from application.", Svc.DEBUG, fileId));
        }
        try {
            final Json response = api.getFileMetadata(fileId);

            if(debug) {
                logger.info(String.format("%s file [%s] metadata received from application - name [%s]", Svc.DEBUG, fileId, response.string(Parameter.FILE_NAME)));
            }
            return response;
        } catch (SvcException rex){
            throw rex;
        } catch (Exception ex){
            throw SvcException.retryable(ErrorCode.CLIENT, String.format("Exception when try to download file [%s]: %s", fileId, ex.getMessage()), ex);
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////
    // helper methods
    ///////////////////////////////////////////////////////////////////////////////////////////////
}
