package io.slingr.services.utils.converters;

import io.slingr.services.utils.EmailUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * These are the allowed content type formats by the Json conversion.
 *
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

    private String mimeType;
    private MediaType mediaType;

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
        return mediaTypes.toArray(new String[mediaTypes.size()]);
    }

    public static boolean isJsonContentType(MediaType contentType){
        if(contentType != null){
            if(JSON.match(contentType)){
                return true;
            }
        }
        return false;
    }

    public static boolean isJsonContentType(String contentType){
        if(StringUtils.isNotBlank(contentType)){
            if(JSON.match(contentType)){
                return true;
            }
        }
        return false;
    }

    public static boolean isUrlEncodedFormContentType(MediaType contentType){
        if(contentType != null){
            if(FORM_URLENCODED.match(contentType)){
                return true;
            }
        }
        return false;
    }

    public static boolean isUrlEncodedFormContentType(String contentType){
        if(StringUtils.isNotBlank(contentType)){
            if(FORM_URLENCODED.match(contentType)){
                return true;
            }
        }
        return false;
    }

    public static boolean isXmlContentType(MediaType contentType){
        if(contentType != null){
            if(XML.match(contentType) || XML_APP.match(contentType) || XML_ATOM.match(contentType) || XML_SVG.match(contentType) || XML_XHTML.match(contentType)){
                return true;
            }
        }
        return false;
    }

    public static boolean isXmlContentType(String contentType){
        if(StringUtils.isNotBlank(contentType)){
            if(XML.match(contentType) || XML_APP.match(contentType) || XML_ATOM.match(contentType) || XML_SVG.match(contentType) || XML_XHTML.match(contentType)){
                return true;
            }
        }
        return false;
    }

    public static boolean isHtmlContentType(MediaType contentType){
        if(contentType != null){
            if(HTML.match(contentType)){
                return true;
            }
        }
        return false;
    }

    public static boolean isHtmlContentType(String contentType){
        if(StringUtils.isNotBlank(contentType)){
            if(HTML.match(contentType)){
                return true;
            }
        }
        return false;
    }

    public static boolean isPlainTextContentType(MediaType contentType){
        if(contentType != null){
            if(PLAIN_TEXT.match(contentType)){
                return true;
            }
        }
        return false;
    }

    public static boolean isPlainTextContentType(String contentType){
        if(StringUtils.isNotBlank(contentType)){
            if(PLAIN_TEXT.match(contentType)){
                return true;
            }
        }
        return false;
    }

    public static boolean isMultipartContentType(MediaType contentType){
        if(contentType != null){
            if(MULTIPART.match(contentType)){
                return true;
            }
        }
        return false;
    }

    public static boolean isMultipartContentType(String contentType){
        if(StringUtils.isNotBlank(contentType)){
            if(MULTIPART.match(contentType)){
                return true;
            }
        }
        return false;
    }
}
