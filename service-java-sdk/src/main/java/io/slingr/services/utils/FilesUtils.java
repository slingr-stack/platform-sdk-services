package io.slingr.services.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Random;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.FileUtils.toFile;

/**
 * Utilities to handle files.
 *
 * Created by dgaviola on 19/1/16.
 */
public class FilesUtils {
    private static final Logger logger = LoggerFactory.getLogger(FilesUtils.class);

    private static final String TEMP_FILE_PREFIX_PATTER = "sl_tmp_%s";
    private static final int BUFFER_8KB = 8 * 1024; // 8,192
    private static final int FLUSH_STEP_1MB = 1024 * 1024; // 1,048,576
    private static final Random RANDOM = new Random();

    /**
     * Reads the content of a file on the local environment
     *
     * @param pathName path of the file to read
     * @return content of the file
     */
    public static String readLocalFile(String pathName) throws IOException {
        if(StringUtils.isBlank(pathName)){
            throw new IllegalArgumentException("Empty path name");
        }
        final File file = new File(pathName);
        if(!file.exists()){
            throw new IllegalStateException(String.format("File [%s] does not exist.", pathName));
        }
        return readFileToString(file, Charset.defaultCharset());
    }

    /**
     * Returns the input stream of a file on the local environment
     *
     * @param pathName path of the file to read
     * @return content of the file
     */
    public static InputStream getLocalFile(String pathName) throws IOException {
        if(StringUtils.isBlank(pathName)){
            throw new IllegalArgumentException("Empty path name");
        }
        final File file = new File(pathName);
        if(!file.exists()){
            throw new IllegalStateException(String.format("File [%s] does not exist.", pathName));
        }
        return new FileInputStream(file);
    }

    /**
     * Reads the content of a file included on the classpath
     *
     * @param filename name of the file to read
     * @return content of the file
     */
    public static String readInternalFile(String filename) throws IOException {
        return readInternalFile(filename, false);
    }

    /**
     * Reads the content of a file included on the classpath
     *
     * @param filename name of the file to read
     * @param absolute true if the path must be absolute
     * @return content of the file
     */
    public static String readInternalFile(String filename, boolean absolute) throws IOException {
        final File file = toFile(getInternalFileURL(filename, absolute));
        if(file == null || !file.exists()){
            throw new IllegalStateException(String.format("Internal file [%s] does not exist.", filename));
        }
        return readFileToString(file, Charset.defaultCharset());
    }

    /**
     * Returns the input stream of a file included on the classpath
     *
     * @param filename name of the file to read
     * @return content of the file
     */
    public static InputStream getInternalFile(String filename) throws IOException {
        return getInternalFile(filename, false);
    }

    /**
     * Returns the input stream of a file included on the classpath
     *
     * @param filename name of the file to read
     * @param absolute true if the path must be absolute
     * @return content of the file
     */
    public static InputStream getInternalFile(String filename, boolean absolute) throws IOException {
        final File file = toFile(getInternalFileURL(filename, absolute));
        if(file == null || !file.exists()){
            throw new IllegalStateException(String.format("File [%s] does not exist.", filename));
        }
        return new FileInputStream(file);
    }

    /**
     * Returns the URL of a file included on the classpath
     *
     * @param filename name of the file to read
     * @param absolute true if the path must be absolute
     * @return URL of the file
     */
    public static URL getInternalFileURL(String filename, boolean absolute) throws IOException {
        if(StringUtils.isBlank(filename)){
            throw new IllegalArgumentException("Empty filename");
        }
        if(absolute && !filename.startsWith("/")){
            filename = "/"+filename;
        }
        return FileUtils.class.getClassLoader().getResource(filename);
    }

    /**
     * Copies the input stream to the output stream. The difference between this method and IOUtils.copy()
     * is that this method flushes periodically (each 1MB), which is good for big files.
     *
     * @param is the input stream
     * @param os the output stream
     * @throws IOException if there is an error while copying the stream
     */
    public static void copyStreamAndFlush(InputStream is, OutputStream os) throws IOException {
        copyStreamAndFlush(is, os, FLUSH_STEP_1MB);
    }

    /**
     * Copies the input stream to the output stream. The difference between this method and IOUtils.copy()
     * is that this method flushes periodically, which is good for big files.
     *
     * @param is the input stream
     * @param os the output stream
     * @param flushStep the number of bytes between flushes
     * @throws IOException if there is an error while copying the stream
     */
    public static void copyStreamAndFlush(InputStream is, OutputStream os, int flushStep) throws IOException {
        byte[] buffer = new byte[BUFFER_8KB];
        int bytesRead, bytesBuffered = 0;
        while ((bytesRead = is.read(buffer)) > -1) {
            os.write(buffer, 0, bytesRead);
            bytesBuffered += bytesRead;
            if (bytesBuffered > flushStep) {
                bytesBuffered = 0;
                os.flush();
            }
        }
        os.flush();
    }

    /**
     * Copies the content of the input stream to a temporary file. The input stream will be closed.
     *
     * @param filename filename of the input stream content
     * @param inputStream content of the file
     * @return temporary file
     */
    public static File copyInputStreamToTemporaryFile(String filename, InputStream inputStream){
        return copyInputStreamToTemporaryFile(filename, inputStream, true);
    }

    /**
     * Copies the content of the input stream to a temporary file.
     *
     * @param filename filename of the input stream content
     * @param inputStream content of the file
     * @param closeStream true if the stream must be closed after read it.
     * @return temporary file
     */
    public static File copyInputStreamToTemporaryFile(String filename, InputStream inputStream, boolean closeStream){
        File tmp = null;
        if(inputStream == null){
            logger.warn(String.format("The input stream for the file [%s] is null. It is not possible to create file.", convertNameForLog(filename)));
        } else {
            try {
                tmp = File.createTempFile(convertNameForTmpFile(filename), null);

                final FileOutputStream tf = new FileOutputStream(tmp);
                FilesUtils.copyStreamAndFlush(inputStream, tf);
                tf.close();

                if(closeStream) {
                    inputStream.close();
                }
            } catch (IOException ex) {
                logger.warn(String.format("An exception occurs when try to save the input stream of the file [%s] - Exception: %s", convertNameForLog(filename), ex.getMessage()), ex);
            }
        }
        return tmp;
    }

    /**
     * Copies the file content to the output stream
     *
     * @param file file to be read
     * @param outputStream output stream
     */
    public static void copyFileToOutputStream(File file, OutputStream outputStream){
        try {
            final InputStream tmpIs = new FileInputStream(file);
            FilesUtils.copyStreamAndFlush(tmpIs, outputStream);
            tmpIs.close();
        } catch (IOException ex) {
            logger.warn(String.format("An exception occurs when try to copy the file [%s] on the output stream - Exception: %s", convertNameForLog(file.getName()), ex.getMessage()), ex);
        }
    }

    /**
     * Generates a string to be used to log a filename
     *
     * @param filename filename
     * @return string to be used on logs
     */
    private static String convertNameForLog(String filename){
        return StringUtils.isNotBlank(filename) ? filename : "-no name-";
    }

    /**
     * Returns a filename to be used in temporary files
     *
     * @param postfix postfix to be used on the filename. if it is empty, a random name will be generated.
     * @return temporary filename
     */
    private static String convertNameForTmpFile(String postfix){
        return String.format(TEMP_FILE_PREFIX_PATTER, StringUtils.isNotBlank(postfix) ? postfix : RANDOM.nextInt(1024));
    }

    /**
     * Generates the media type using the given information.
     *
     * @param contentType contentType of the file
     * @return media type
     */
    public static MediaType getMediaTypeForMultipart(String contentType, String filename){
        MediaType mediaType = getMediaType(contentType, filename);
        if(mediaType == null){
            mediaType = getMediaType(contentType, filename);
        }
        return mediaType;
    }

    /**
     * Generates the media type using the given information.
     *
     * @param contentType contentType of the file
     * @return media type
     */
    public static MediaType getMediaType(String contentType){
        return getMediaType(contentType, null);
    }

    /**
     * Generates the media type using the given information.
     *
     * @param contentType contentType of the file
     * @param filename file name
     * @return media type
     */
    public static MediaType getMediaType(String contentType, String filename){
        final String normalizedContentType = getContentType(contentType, filename);

        MediaType mediaType = null;
        if(StringUtils.isNotBlank(normalizedContentType)) {
            try {
                mediaType = MediaType.valueOf(normalizedContentType);
            } catch (Exception ex){
                logger.warn(String.format("Error when try to generate media type from content type [%s]. Exception [%s]", contentType, ex.getMessage()));
            }
        }
        return mediaType;
    }

    /**
     * Generates the content type using the given information.
     *
     * @param contentType contentType of the file
     * @param filename file name
     * @return content type
     */
    public static String getContentType(String contentType, String filename){
        if(StringUtils.isBlank(contentType)){
            contentType = URLConnection.guessContentTypeFromName(filename);
            if(StringUtils.isBlank(contentType)) {
                contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(filename);
            }
        }
        return contentType;
    }
}
