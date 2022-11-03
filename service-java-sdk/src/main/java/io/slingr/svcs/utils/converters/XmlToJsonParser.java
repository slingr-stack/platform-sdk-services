package io.slingr.svcs.utils.converters;

import io.slingr.svcs.exceptions.SvcException;
import io.slingr.svcs.exceptions.ErrorCode;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.utils.XmlUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <p>Converts the XML documents to the equivalent Json objects. Rules to convert are based on
 * <a href="https://developer.mozilla.org/en-US/docs/JXON">the JXON principles.</a>
 *
 * <p>Created by lefunes on 28/09/15.
 */
public class XmlToJsonParser extends DefaultHandler {

    final static Pattern NUMBER_PATTERN = Pattern.compile("\\s*[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?\\s*");

    private Json response = Json.map();
    private final List<Json> stack = new ArrayList<>();

    /**
     * Converts the XML document to an equivalent Json object using the
     * <a href="https://developer.mozilla.org/en-US/docs/JXON">the JXON principles.</a>
     *
     * @param xml XML document to convert
     * @return equivalent Json
     * @throws SvcException if any error happens
     */
    public static Json parse(String xml) throws SvcException {
        try {
            return new XmlToJsonParser(xml).getResponse();
        } catch (Exception ex){
            throw SvcException.permanent(ErrorCode.CONVERSION, ex.getMessage(), ex);
        }
    }

    private XmlToJsonParser(final String xml) throws SvcException {
        try {
            final XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(this);
            xr.setErrorHandler(this);
            xr.setEntityResolver(this);

            xr.parse(new InputSource(new ByteArrayInputStream(xml.getBytes())));
        } catch (Exception ex){
            throw SvcException.permanent(ErrorCode.CONVERSION, ex.getMessage(), ex);
        }
    }

    public Json getResponse() {
        return response;
    }

    public String attributeName(String name){
        if(StringUtils.isNotBlank(name)) {
            return String.format("@%s", name);
        }
        return null;
    }

    @Override
    public InputSource resolveEntity (String publicId, String systemId){
        // this helps to avoid SAX try to download DTD files
        return new InputSource(new StringReader(""));
    }

    @Override
    public void startDocument() {
        // creates the root document
        stack.clear();
        stack.add(0, Json.map());
    }

    @Override
    public void endDocument() {
        // put the root document as result of the conversion
        response = stack.get(0);
    }

    @Override
    public void startElement(String uri, String elementName, String qName, Attributes attributes) {
        // creates a new element
        final Json work = Json.map();

        if(attributes != null && attributes.getLength() > 0) {
            // save the element attributes with the prefix @
            for (int i = 0; i < attributes.getLength(); i++) {
                final String aName = attributeName(attributes.getLocalName(i));
                if (StringUtils.isNotBlank(aName)) {
                    work.setIfNotEmpty(aName, attributes.getValue(i));
                }
            }
        }

        // store it on the stack to use on the endElement method
        stack.add(0, work);
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        // characters that are inside of the element [<element>Hello world</element>].
        // if the characters are not contiguous, the method is called more that once,
        // [<element>Hello <test /> world</element>] is called two times with "Hello " and " world"

        // get the element from the stack
        final Json currentElement = stack.get(0);
        if(currentElement != null) {
            // new value to add
            String newValue = new String(ch, start, length);
            if(StringUtils.isEmpty(newValue)){
                newValue = "";
            }
            // old value
            String value = currentElement.string(XmlUtils.TEXT_KEY);
            if(StringUtils.isEmpty(value)){
                value = "";
            }

            // store the concatenated value as the text of the element
            currentElement.set(XmlUtils.TEXT_KEY, String.format("%s%s", value, newValue));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void endElement(String uri, String elementName, String qName) {
        // restore the element from the stack
        final Json currentElement = stack.remove(0);

        Object elementToStore = currentElement;
        if(currentElement.isEmpty()){
            // use the '' value if the element is empty (either [<element />] or [<element></element>])
            elementToStore = "";

        } else if (currentElement.contains(XmlUtils.TEXT_KEY)) {
            // get the text value
            String value = currentElement.string(XmlUtils.TEXT_KEY);
            if(StringUtils.isNotBlank(value)){
                // the multiple spaces are simplified to avoid errors at time to build the text string
                // [<element>Hello world</element>] is converted to "Hello world"
                // [<element>   Hello    world    </element>] is converted to "Hello world"
                // [<element>Hello <other/> world</element>] is converted to "Hello world"
                // [<element> He<other1/>llo <other2/> wo<other3/>rld <other4/> </element>] is converted to "Hello world"

                value = value.replaceAll("\\s+", " ").trim();
            }

            if (currentElement.size()==1){
                if (StringUtils.isBlank(value)) {
                    // use the '' value if the element has empty text only [<element>     </element>]
                    elementToStore = "";

                } else {
                    // use the text value if this is the unique value of the element [<element>Hello world</element>]
                    elementToStore = convertToPrimitiveValue(value);
                }

            } else {
                if (StringUtils.isBlank(value)) {
                    // remove the text if this is empty [<element>  <other /> </element>]
                    currentElement.remove(XmlUtils.TEXT_KEY);

                } else {
                    // save the modified value of the text
                    currentElement.set(XmlUtils.TEXT_KEY, convertToPrimitiveValue(value));
                }
            }
        }

        final Json parentElement = stack.get(0);
        if(parentElement != null) {
            if (parentElement.isEmpty() || !parentElement.contains(elementName)) {
                // add the new object
                parentElement.set(elementName, elementToStore);

            } else {
                // there is a element with the same name, create a list or add to the existent one
                Object elementWithSameName = parentElement.object(elementName);
                if (elementWithSameName instanceof Json) {
                    ((Json) elementWithSameName).push(elementToStore);
                } else if(elementWithSameName instanceof List){
                    ((List) elementWithSameName).add(elementToStore);
                } else {
                    final List list = new ArrayList();
                    list.add(elementWithSameName);
                    list.add(elementToStore);
                    parentElement.set(elementName, list);
                }
            }
        }
    }

    private Object convertToPrimitiveValue(final String value) {
        Object newValue = value;
        if(StringUtils.isNotBlank(value)) {
            if(value.equalsIgnoreCase("true")){
                newValue = true;
            } else if(value.equalsIgnoreCase("false")){
                newValue = false;
            } else {
                String newNumber = value.trim().replaceAll("-\\s*", "-");

                if(NUMBER_PATTERN.matcher(newNumber).matches()){
                    Number number = null;
                    try {
                        number = NumberFormat.getInstance().parse(newNumber);
                    } catch (ParseException e) {
                        // do nothing
                    } finally {
                        if (number != null) {
                            newValue = number;
                        }
                    }
                }
            }
        } else {
            newValue = true;
        }
        return newValue;
    }
}