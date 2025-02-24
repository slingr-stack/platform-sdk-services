package io.slingr.services.utils.converters;

import io.slingr.services.utils.EmailUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * These are the allowed content type formats by the Json conversion.
 * <p>
 * Created by dgaviola on 22/08/15.
 */
public enum ContentTypeFormat {
    JSON(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_TYPE),
    FORM_URLENCODED(MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_FORM_URLENCODED_TYPE),
    XML(MediaType.TEXT_XML, MediaType.TEXT_XML_TYPE),
    XML_APP(MediaType.APPLICATION_XML, MediaType.APPLICATION_XML_TYPE),
    XML_ATOM(MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_ATOM_XML_TYPE),
    XML_SVG(MediaType.APPLICATION_SVG_XML, MediaType.APPLICATION_SVG_XML_TYPE),
    XML_XHTML(MediaType.APPLICATION_XHTML_XML, MediaType.APPLICATION_XHTML_XML_TYPE),
    HTML(MediaType.TEXT_HTML, MediaType.TEXT_HTML_TYPE),
    PLAIN_TEXT(MediaType.TEXT_PLAIN, MediaType.TEXT_PLAIN_TYPE),
    MULTIPART(MediaType.MULTIPART_FORM_DATA, MediaType.MULTIPART_FORM_DATA_TYPE)
    ;

    private final String mimeType;
    private final MediaType mediaType;

    ContentTypeFormat(String mimeType, MediaType mediaType) {
        this.mimeType = mimeType;
        this.mediaType = mediaType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public boolean match(String contentTypeString) {
        final String contentType = EmailUtils.getContentType(contentTypeString);
        return mimeType.equalsIgnoreCase(contentType);
    }

    public boolean match(MediaType contentType) {
        return mediaType.isCompatible(contentType);
    }

    public static String[] getAcceptedFormats(){
        final List<String> mediaTypes = new ArrayList<>();
        for (ContentTypeFormat contentTypeFormat : values()) {
            mediaTypes.add(contentTypeFormat.getMimeType());
        }
        return mediaTypes.toArray(new String[0]);
    }

    public static boolean isJsonContentType(MediaType contentType){
        if(contentType != null){
            return JSON.match(contentType);
        }
        return false;
    }

    public static boolean isJsonContentType(String contentType){
        if(StringUtils.isNotBlank(contentType)){
            return JSON.match(contentType);
        }
        return false;
    }

    public static boolean isUrlEncodedFormContentType(MediaType contentType){
        if(contentType != null){
            return FORM_URLENCODED.match(contentType);
        }
        return false;
    }

    public static boolean isUrlEncodedFormContentType(String contentType){
        if(StringUtils.isNotBlank(contentType)){
            return FORM_URLENCODED.match(contentType);
        }
        return false;
    }

    public static boolean isXmlContentType(MediaType contentType){
        if(contentType != null){
            return XML.match(contentType) || XML_APP.match(contentType) || XML_ATOM.match(contentType) || XML_SVG.match(contentType) || XML_XHTML.match(contentType);
        }
        return false;
    }

    public static boolean isXmlContentType(String contentType){
        if(StringUtils.isNotBlank(contentType)){
            return XML.match(contentType) || XML_APP.match(contentType) || XML_ATOM.match(contentType) || XML_SVG.match(contentType) || XML_XHTML.match(contentType);
        }
        return false;
    }

    public static boolean isHtmlContentType(MediaType contentType){
        if(contentType != null){
            return HTML.match(contentType);
        }
        return false;
    }

    public static boolean isHtmlContentType(String contentType){
        if(StringUtils.isNotBlank(contentType)){
            return HTML.match(contentType);
        }
        return false;
    }

    public static boolean isPlainTextContentType(MediaType contentType){
        if(contentType != null){
            return PLAIN_TEXT.match(contentType);
        }
        return false;
    }

    public static boolean isPlainTextContentType(String contentType){
        if(StringUtils.isNotBlank(contentType)){
            return PLAIN_TEXT.match(contentType);
        }
        return false;
    }

    public static boolean isMultipartContentType(MediaType contentType){
        if(contentType != null){
            return MULTIPART.match(contentType);
        }
        return false;
    }

    public static boolean isMultipartContentType(String contentType){
        if(StringUtils.isNotBlank(contentType)){
            return MULTIPART.match(contentType);
        }
        return false;
    }
}