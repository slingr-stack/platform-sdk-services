package io.slingr.services.utils.converters;

import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.XmlUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * <p>Converts the Json objects to the equivalent XML documents. Rules to convert are based on
 * <a href="https://developer.mozilla.org/en-US/docs/JXON">the JXON principles.</a>
 *
 * <p>Created by lefunes on 29/09/15.
 */
public class JsonToXmlParser {

    /**
     * Converts the Json to an equivalent XML document using the
     * <a href="https://developer.mozilla.org/en-US/docs/JXON">the JXON principles.</a>
     *
     * @param json Json object to convert
     * @return equivalent XML document
     * @throws ServiceException if any error happens
     */
    public static String parse(Json json) {
        try {
            return parse(json, true);
        } catch (Exception ex){
            throw ServiceException.permanent(ErrorCode.CONVERSION, ex.getMessage(), ex);
        }
    }

    /**
     * Converts the Json to an equivalent XML document using the
     * <a href="https://developer.mozilla.org/en-US/docs/JXON">the JXON principles.</a>
     *
     * @param json Json object to convert
     * @param complete if this is false, the XML header and tabulation are ignored
     * @return equivalent XML document
     * @throws ServiceException if any error happens
     */
    public static String parse(Json json, boolean complete) {
        final StringBuilder xml = new StringBuilder();
        if(complete) {
            xml.append("<?xml version=\"1.0\"?>\n");
        }
        if(json != null && !json.isEmpty()){
            parse(xml, json, complete);
        }
        return xml.toString();
    }

    private static void parse(StringBuilder xml, Json json, boolean complete){
        parseElement(xml, "root", json, 0, complete, true);
    }

    private static void parseElement(StringBuilder xml, String name, Object value, int level, boolean complete, boolean root){
        // generate tabulation and end of line characters according to the parameters
        final StringBuilder iniString = new StringBuilder();
        String endString = "";
        if(complete){
            for (int i = 0; i < level; i++){
                iniString.append("\t");
            }
            endString = "\n";
        }

        // if the element is a Json like object
        if(value instanceof Json || value instanceof Map || value instanceof List) {
            final Json json = Json.fromObject(value);

            if(json.isMap()) {
                // separate options (text and cdata), elements and arguments
                final Json attributes = Json.map();
                final Json elements = Json.map();
                String lastElement = null;

                boolean cdata = false;
                String text = "";

                for (String key : json.keys()) {
                    if (XmlUtils.TEXT_KEY.equals(key)) {
                        text = json.string(XmlUtils.TEXT_KEY);
                    } else if (XmlUtils.USE_CDATA.equals(key)) {
                        cdata = json.bool(XmlUtils.USE_CDATA, false);
                    } else if (key.startsWith("@")) {
                        attributes.set(key.substring(1), json.string(key));
                    } else {
                        lastElement = key;
                        elements.set(lastElement, json.object(key));
                    }
                }

                if(cdata){
                    text = String.format("<![CDATA[%s]]>", text);
                }

                boolean parsed = false;
                if (root) {
                    // if the root element contains only 1 element and any parameter and text, ignore it and start
                    // the xml with his child  [{"child": "value"} => <child>value</child>]
                    if (StringUtils.isBlank(text) && attributes.isEmpty() && elements.size() == 1) {
                        parseElement(xml, lastElement, json.object(lastElement), 0, complete, false);
                        parsed = true;
                    }
                }

                if (!parsed) {
                    // initial tag (w/ attributes) and text
                    xml.append(iniString).append(iniElement(name, attributes)).append(text);

                    if(!elements.isEmpty()) {
                        xml.append(endString);

                        // parse any element
                        for (String element : elements.keys()) {
                            parseElement(xml, element, json.object(element), level + 1, complete, false);
                        }

                        xml.append(iniString);
                    }

                    // end tag
                    xml.append(endElement(name)).append(endString);
                }
            } else {
                // the element is a list, use the same name to generate each children
                // [{"dog":["a", "b"]}  =>  <dog>A</dog><dog>B</dog>]
                for (Object element : json.objects()) {
                    parseElement(xml, name, element, level, complete, false);
                }
            }
        } else {
            // this is a simple element, add it to the xml
            xml.append(iniString);
            if(value == null){
                // empty element [<element />]
                xml.append(emptyElement(name));
            } else {
                // simple element without arguments [<element>value</element>]
                xml.append(simpleElement(name, value.toString()));
            }
            xml.append(endString);
        }
    }

    private static String emptyElement(String name){
        return emptyElement(name, null);
    }

    private static String emptyElement(String name, Json attributes){
        return String.format("<%s%s />", name, appendAttributes(attributes));
    }

    private static String simpleElement(String name, String value){
        return simpleElement(name, null, value);
    }

    private static String simpleElement(String name, Json attributes, String value){
        return String.format("%s%s%s", iniElement(name, attributes), value, endElement(name));
    }

    private static String iniElement(String name, Json attributes){
        return String.format("<%s%s>", name, appendAttributes(attributes));
    }

    private static String endElement(String name){
        return String.format("</%s>", name);
    }

    private static String appendAttributes(Json attributes) {
        final StringBuilder sb = new StringBuilder();
        if(attributes != null){
            for (String key : attributes.keys()) {
                sb.append(" ").append(key).append("=\"").append(attributes.string(key)).append("\"");
            }
        }
        return sb.toString();
    }
}
