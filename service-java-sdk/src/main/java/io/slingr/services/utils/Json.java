package io.slingr.services.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.slingr.services.utils.converters.JsonConverter;
import io.slingr.services.utils.converters.JsonSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Form;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Class that makes it easy to create JSON objects. For example:
 * <p/>
 * <pre><code>
 * Json.map()
 *   .set("prop1", val1)
 *   .set("prop2", val2)
 *   .set("nested1", Json.map()
 *     .set("prop1", val1)
 *     .set("prop2", val2))
 *   .set("array1", Json.list()
 *     .push("element1")
 *     .push("element2"))
 *   .set("array2", Json.list(users, new Json.ListGenerator() {
 *       public Object element(UserRecorduser) {
 *         return Json.map()
 *           .set("name", user.getFirstName() + " " + user.getLastName())
 *           .set("email", user.getEmail());
 *       }
 *     })
 *   );
 * </code></pre>
 * <p/>
 * Arrays are still a bit cumbersome, but hopefully this will be better with lambdas in Java 8.
 * <p/>
 * User: dgaviola
 * Date: 1/21/13
 */
public class Json implements JsonSource {
    private static final Logger logger = LoggerFactory.getLogger(Json.class);

    private final Map<String, Object> map;
    private final List<Object> list;
    private final boolean isMap;

    /**
     * Creates a Json objects
     *
     * @param isMap true if the Json represents a Map
     */
    private Json(boolean isMap) {
        this.isMap = isMap;
        if(isMap){
            map = new LinkedHashMap<>();
            list = null;
        } else {
            map = null;
            list = new ArrayList<>();
        }
    }

    /**
     * Builds an empty json map
     *
     * @return the json map
     */
    public static Json map() {
        return new Json(true);
    }

    /**
     * Builds a json map filled with the values processed by the generator
     *
     * @return the json map
     */
    public static <T> Json map(T obj, MapGenerator<T> mapGenerator) {
        return map().generate(obj, mapGenerator);
    }

    /**
     * Builds an empty json list
     *
     * @return the json list
     */
    public static Json list() {
        return new Json(false);
    }

    /**
     * Builds a json list filled with the values processed by the generator
     *
     * @return the json list
     */
    public static <T> Json list(Collection<T> items, ListGenerator<T> listGenerator) {
        return list().generate(items, listGenerator);
    }

    /**
     * Builds a json map filled with the values of the map
     *
     * @param map map to generate the json
     * @return the json map
     */
    public static Json fromMap(Map<String, ?> map) {
        final Json json = map();
        if (map != null) {
            for (Map.Entry<String, ?> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (value != null) {
                    final Json jsonObject = objectItemToJson(value);
                    if(jsonObject != null){
                        value = jsonObject;
                    }
                }
                json.map.put(entry.getKey(), value);
            }
        }
        return json;
    }

    /**
     * Builds a json list filled with the values of the list
     *
     * @param list list to generate the json
     * @return the json list
     */
    public static Json fromList(Iterable<Object> list) {
        final Json json = list();
        if (list != null) {
            for (Object item : list) {
                Object value = item;
                if (item != null) {
                    final Json jsonObject = objectItemToJson(item);
                    if(jsonObject != null){
                        value = jsonObject;
                    }
                }
                json.list.add(value);
            }
        }
        return json;
    }

    /**
     * Builds a json list filled with the values of the Enumeration
     *
     * @param list list to generate the json
     * @return the json list
     */
    public static Json fromEnumeration(Enumeration list) {
        if(list != null) {
            return fromList(Collections.list(list));
        } else {
            return Json.list();
        }
    }

    /**
     * Builds a json filled with the internal values of the object.
     * Internal object will be cloned.
     *
     * @param object object to use to generate the json
     * @return the json
     */
    public static Json fromObject(Object object){
        return fromObject(object, true);
    }

    /**
     * Builds a json filled with the internal values of the object.
     *
     * @param object object to use to generate the json
     * @param clone true if you want to clone internal objects
     * @return the json
     */
    public static Json fromObject(Object object, boolean clone){
        return fromObject(object, clone, false);
    }

    /**
     * Builds a json filled with the internal values of the object.
     *
     * @param object object to use to generate the json
     * @param clone true if you want to clone internal objects
     * @return the json
     */
    public static Json fromObject(Object object, boolean clone, boolean returnsNullIfInvalid){
        if(object != null) {
            if (object instanceof Json) {
                if (clone) {
                    return ((Json) object).cloneJson();
                } else {
                    return ((Json) object);
                }
            } else {
                try {
                    return JsonConverter.baseFromObject(object, !returnsNullIfInvalid);
                } catch (Exception ex){
                    if(returnsNullIfInvalid){
                        return null;
                    } else {
                        throw ex;
                    }
                }
            }
        }
        return returnsNullIfInvalid ? null : map();
    }

    /**
     * Builds a json using the string content.
     *
     * @param jsonString string content
     * @return the json
     */
    public static Json parse(String jsonString) {
        return parse(jsonString, true);
    }

    /**
     * Builds a json using the string content.
     *
     * @param jsonString string content
     * @param showErrors true if want to show warnings on console
     * @return the json
     */
    public static Json parse(String jsonString, boolean showErrors) {
        // if the string starts with a square bracket we assume it is an array
        if (StringUtils.isNotBlank(jsonString) && jsonString.trim().startsWith("[")) {
            return fromList(stringToList(jsonString, showErrors));
        } else {
            return fromMap(stringToMap(jsonString, showErrors));
        }
    }

    /**
     * Builds a json map using the string content.
     *
     * @param jsonString string content
     * @return the json map
     */
    public static Map stringToMap(String jsonString) {
        return stringToMap(jsonString, true);
    }

    /**
     * Builds a json map using the string content.
     *
     * @param jsonString string content
     * @param showErrors true if want to show warnings on console
     * @return the json map
     */
    public static Map stringToMap(String jsonString, boolean showErrors) {
        Map result = stringToObject(jsonString, Map.class, showErrors);
        if(result == null) {
            result = new LinkedHashMap();
        }
        return result;
    }

    /**
     * Builds a json list using the string content.
     *
     * @param jsonString string content
     * @return the json list
     */
    public static List stringToList(String jsonString) {
        return stringToList(jsonString, true);
    }

    /**
     * Builds a json list using the string content.
     *
     * @param jsonString string content
     * @param showErrors true if want to show warnings on console
     * @return the json list
     */
    public static List stringToList(String jsonString, boolean showErrors) {
        List result = stringToObject(jsonString, List.class, showErrors);
        if(result == null) {
            result = new ArrayList();
        }
        return result;
    }

    /**
     * Builds a json using the string content.
     *
     * @param jsonString string content
     * @return the json
     */
    public static <T> T stringToObject(String jsonString, Class<T> valueType) {
        return stringToObject(jsonString, valueType, true);
    }

    /**
     * Builds a json using the string content.
     *
     * @param jsonString string content
     * @param showErrors true if want to show warnings on console
     * @return the json
     */
    public static <T> T stringToObject(String jsonString, Class<T> valueType, boolean showErrors) {
        if (StringUtils.isBlank(jsonString)) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonString, valueType);
        } catch (UnrecognizedPropertyException upe) {
            throw new RuntimeException(String.format("Unrecognized field [%s]", upe.getPropertyName()));
        } catch (IOException e) {
            // converts the exception to an unchecked exception to make it more convenient.
            if(showErrors) {
                logger.trace(String.format("Error parsing JSON string: %s", jsonString));
            }
            throw new RuntimeException("Error parsing JSON string", e);
        }
    }

    /**
     * Reads and parses the content of a file on the local environment
     *
     * @param pathName path of the file to read
     * @return parsed json
     */
    public static Json fromLocalFile(String pathName) throws IOException {
        return parse(FilesUtils.readLocalFile(pathName));
    }

    /**
     * Reads and parses the content of a file included on the classpath
     *
     * @param filename name of the file to read
     * @return parsed json
     */
    public static Json fromInternalFile(String filename) throws IOException {
        return fromInternalFile(filename, false);
    }

    /**
     * Reads and parses the content of a file included on the classpath
     *
     * @param filename name of the file to read
     * @param absolute true if the path must be absolute
     * @return parsed json
     */
    public static Json fromInternalFile(String filename, boolean absolute) throws IOException {
        return parse(FilesUtils.readInternalFile(filename, absolute));
    }

    /**
     * True if the json implements a map
     */
    public boolean isMap() {
        return isMap;
    }

    /**
     * True if the json implements a list
     */
    public boolean isList() {
        return !isMap;
    }

    /**
     * True is the json is empty
     */
    public boolean isEmpty() {
        if (isMap()) {
            return map.isEmpty();
        } else {
            return list.isEmpty();
        }
    }

    /**
     * True if the json is not empty
     */
    public boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * Returns the number of elements included on the json
     */
    public int size() {
        if (isMap()) {
            return map.size();
        } else {
            return list.size();
        }
    }

    /**
     * Fills the json map with the values processed by the generator
     *
     * @return the json map
     */
    public <T> Json generate(T obj, MapGenerator<T> mapGenerator) {
        notSupportedByList("generate map");

        if (obj != null && mapGenerator != null) {
            mapGenerator.process(obj, this);
        }
        return this;
    }

    /**
     * Generator that adds elements to the json map
     */
    public interface MapGenerator<T> {
        /**
         * Processes the given object and adds new values on the map based on it
         *
         * @param object object to process
         * @param map map to include new values
         */
        void process(T object, Json map);
    }

    /**
     * Fills the json list with the values processed by the generator
     *
     * @return the json list
     */
    public <T> Json generate(Collection<T> items, ListGenerator<T> listGenerator) {
        notSupportedByMap("generate list");

        if (items != null && listGenerator != null) {
            for (T item : items) {
                pushIfNotNull(listGenerator.element(item));
            }
        }
        return this;
    }

    /**
     * Generator that adds elements to the json list
     */
    public interface ListGenerator<T> {
        /**
         * Processes the item to add on the json list. Null values not be included on list.
         * @param item item to process
         * @return value to push on list. null value does not be included.
         */
        Object element(T item);
    }

    /**
     * Performs the given action for each entry in this map until all entries
     * have been processed or the action throws an exception.
     *
     * @param action The action to be performed for each entry
     */
    public void forEachMap(BiConsumer<? super String, ? super Object> action){
        notSupportedByList("for each map");

        this.map.forEach(action);
    }

    /**
     * Performs the given action for each entry in this map until all entries
     * have been processed or the action throws an exception.
     *
     * @param action The action to be performed for each entry
     */
    public void forEachMapString(BiConsumer<? super String, ? super String> action){
        notSupportedByList("for each map string");

        this.toMapString().forEach(action);
    }

    /**
     * Performs the given action for each element of the json list
     * until all elements have been processed or the action throws an
     * exception.
     *
     * @param action The action to be performed for each element
     */
    public void forEachList(Consumer<? super Object> action){
        notSupportedByMap("for each list");

        this.list.forEach(action);
    }

    /**
     * Returns a sequential {@code Stream} with this list as its source.
     *
     * @return a sequential {@code Stream} over the elements in the list
     */
    public Stream<Object> streamList(){
        notSupportedByMap("stream list");

        return this.list.stream();
    }

    /**
     * Clone a Json object. This is not a deep clone of all the internal objects.
     *
     * @return the cloned json
     */
    public Json cloneJson(){
        if(isMap()){
            return fromMap(toMap());
        } else {
            return fromList(toList());
        }
    }

    /**
     * Exports the json as a map of objects. The contained json are converted to map/list objects.
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        notSupportedByList("to map");

        final Map<String, Object> response = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            response.put(entry.getKey(), jsonItemToObject(entry.getValue()));
        }
        return response;
    }

    /**
     * Exports the json as a map of strings
     *
     * @return map
     */
    public Map<String, String> toMapString() {
        notSupportedByList("to map string");

        final Map<String, String> response = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            final Object value = jsonItemToObject(entry.getValue());
            response.put(entry.getKey(), value != null ? value.toString() : null);
        }
        return response;
    }

    /**
     * Exports the json as a list of objects. The contained json are converted to map/list objects.
     *
     * @return list
     */
    public List<Object> toList() {
        notSupportedByMap("to list");

        final List<Object> response = new ArrayList<>();
        for (Object item : list) {
            response.add(jsonItemToObject(item));
        }
        return response;
    }

    /**
     * Exports the json as a map or list
     *
     * @return map or list
     */
    public Object toObject() {
        if(isMap()){
            return toMap();
        } else {
            return toList();
        }
    }

    /**
     * Adds a value to the end of the json list
     *
     * @param value value to add
     * @return the json list
     */
    public Json push(Object value) {
        notSupportedByMap("push");

        list.add(value);
        return this;
    }

    /**
     * Adds a value to the end of the json list if the value is not null
     *
     * @param value value to add
     * @return the json list
     */
    public Json pushIfNotNull(Object value) {
        if (value != null) {
            return push(value);
        }
        return this;
    }

    /**
     * Adds a value to the end of the json list if the value is not empty
     *
     * @param value value to add
     * @return the json list
     */
    public Json pushIfNotEmpty(Object value) {
        return pushIfNotNull(returnIfNotEmpty(value));
    }

    /**
     * Adds the given property/value pair to the json map
     *
     * @param property name of the property
     * @param value value to include
     * @return the json map
     */
    public Json set(String property, Object value) {
        notSupportedByList("set");
        notBlankPropertyName(property);

        if(value instanceof Json){
            set(property, ((Json) value).toObject());
        } else if(value instanceof List){
            List<Object> list = new ArrayList<>();
            for (Object o : (List) value) {
                if(o instanceof Json){
                    list.add(((Json) o).toObject());
                } else if(o != null) {
                    list.add(o);
                }
            }
            map.put(property, list);
        } else if(value != null) {
            map.put(property, value);
        } else {
            // if value is empty, remove property
            map.remove(property);
        }
        return this;
    }

    /**
     * Adds the given property/value pair to the json map if the value is not empty
     *
     * @param property name of the property
     * @param value value to include
     * @return the json map
     */
    public Json setIfNotEmpty(String property, Object value) {
        return setIfNotNull(property, returnIfNotEmpty(value));
    }

    /**
     * Adds the given property/value pair to the json map if the value is not null
     *
     * @param property name of the property
     * @param value value to include
     * @return the json map
     */
    public Json setIfNotNull(String property, Object value) {
        return setIf(value != null, property, value);
    }

    /**
     * Adds the given property/value pair to the json map if the condition is true
     *
     * @param condition condition to check to insert the element on the json
     * @param property name of the property
     * @param value value to include
     * @return the json map
     */
    public Json setIf(boolean condition, String property, Object value) {
        if (condition) {
            set(property, value);
        }
        return this;
    }

    /**
     * Removes a value of the json map using the property name
     *
     * @param property name of the property
     * @return the json map
     */
    public Json remove(String property) {
        notSupportedByList("remove");
        notBlankPropertyName(property);

        map.remove(property);
        return this;
    }

    /**
     * Gets the internal list representation of the json list.
     * If you want to convert all the contained jsons in map/list objects, you should use {@link Json#toList()}
     * instead this method.
     *
     * @return internal list implementation
     */
    public List<Object> objects() {
        notSupportedByMap("objects list");

        return list;
    }

    /**
     * Returns the element at the specified position in the json list.
     *
     * @param index index of the element to return
     * @return the element at the specified position in the json list
     */
    public Object object(int index) {
        notSupportedByMap("object by index");

        return list.get(index);
    }

    /**
     * Gets all the objects as json in the json list. the invalid or null values are ignored.
     *
     * @return list of json objects
     */
    public List<Json> jsons() {
        return jsons(true);
    }

    /**
     * Gets all the objects as json in the json list
     *
     * @param ignoreInvalidItems true if want to ignore the invalid or null values
     * @return list of json objects
     */
    public List<Json> jsons(boolean ignoreInvalidItems) {
        notSupportedByMap("jsons list");

        final List<Json> response = new ArrayList<>();
        for (Object object : list) {
            if(object == null){
                if(!ignoreInvalidItems) {
                    response.add(null);
                }
            } else {
                final Json item = objectItemToJson(object);
                if (item != null) {
                    response.add(item);
                } else {
                    if(!ignoreInvalidItems) {
                        throw new IllegalStateException("Objects on list cannot be converted to JSON");
                    }
                }
            }
        }
        return response;
    }

    /**
     * Returns true if the the object is present on json list
     *
     * @param object object to find
     * @return true if the object is present on list
     */
    public boolean containsObject(Object object) {
        return containsObject(true, object);
    }

    /**
     * Returns true if the the object is present on json list
     *
     * @param validations check support and given parameters
     * @param object object to find
     * @return true if the object is present on list
     */
    private boolean containsObject(boolean validations, Object object) {
        if(validations) {
            notSupportedByMap("contains object");
        }

        return list.contains(object);
    }

    /**
     * Gets the internal map representation of json map.
     * Try to use {@link Json#toMap()} instead this method.
     *
     * @return internal map representation
     */
    public Map<String, Object> objectsMap() {
        notSupportedByList("objects map");

        return map;
    }

    /**
     * Returns a {@link Set} view of the keys contained in the map.
     *
     * @return a set view of the keys contained in the map
     */
    public Set<String> keys() {
        return keys(true);
    }

    /**
     * Returns a {@link Set} view of the keys contained in the map.
     *
     * @param validations check support and given parameters
     * @return a set view of the keys contained in the map
     */
    private Set<String> keys(boolean validations) {
        if(validations) {
            notSupportedByList("keys");
        }

        return map.keySet();
    }

    /**
     * Returns true if the the property is present on json map
     *
     * @param property name of the property
     * @return true if the property is present on map
     */
    public boolean contains(String property) {
        return contains(true, property);
    }

    /**
     * Returns true if the the property is present on json map
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return true if the property is present on map
     */
    private boolean contains(boolean validations, String property) {
        if(validations) {
            notSupportedByList("contains");
            notBlankPropertyName(property);
        }

        return map.containsKey(property);
    }

    /**
     * Returns the value to which the specified property is mapped, or {@code null} if the json map contains no mapping
     * for the property.
     *
     * @param property name of the property
     * @return property value or null
     */
    public Object object(String property) {
        return object(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped, or {@code defaultValue} if the json map contains no
     * mapping for the property.
     *
     * @param property name of the property
     * @return property value or {@code defaultValue}
     */
    public Object object(String property, Object defaultValue) {
        return object(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped, or {@code null} if the json map contains no mapping
     * for the property.
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return property value or null
     */
    private Object object(boolean validations, String property) {
        if(validations) {
            notSupportedByList("object");
            notBlankPropertyName(property);
        }

        return map.get(property);
    }

    /**
     * Returns the value to which the specified property is mapped, or {@code defaultValue} if the json map contains no
     * mapping for the property.
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return property value or {@code defaultValue}
     */
    private Object object(boolean validations, String property, Object defaultValue) {
        Object r = null;
        try {
            r = object(validations, property);
        } catch (Exception ex) {
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Check if the property exists on the json map
     *
     * @param property name of the property
     * @return true if the property value is empty or does not exist on json map
     */
    public boolean isEmpty(String property) {
        return isEmpty(true, property);
    }

    /**
     * Check if the property exists on the json map
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return true if the property value is empty or does not exist on json map
     */
    private boolean isEmpty(boolean validations, String property) {
        if(validations) {
            notSupportedByList("is empty property");
            notBlankPropertyName(property);
        }

        return isEmptyProperty(map, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a json object, otherwise {@code null}
     *
     * @param property name of the property
     * @return the json object or null
     */
    public Json json(String property) {
        return json(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a json object, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the json object or {@code defaultValue}
     */
    public Json json(String property, Json defaultValue) {
        return json(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a json object, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the json object or null
     */
    public Json json(boolean validations, String property) {
        if(validations) {
            notSupportedByList("json");
            notBlankPropertyName(property);
        }

        final Object obj = object(false, property);
        if (obj == null) {
            return null;
        }
        return fromObject(obj, false);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a json object, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the json object or {@code defaultValue}
     */
    public Json json(boolean validations, String property, Json defaultValue) {
        Json r = null;
        try {
            r = json(validations, property);
        } catch (Exception ex) {
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of objects, otherwise {@code null}
     *
     * @param property name of the property
     * @return the list of objects or null
     */
    public List<Object> objects(String property) {
        return objects(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of objects, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of objects or {@code defaultValue}
     */
    public List<Object> objects(String property, List<Object> defaultValue) {
        return objects(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of objects, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the list of objects or null
     */
    private List<Object> objects(boolean validations, String property) {
        if(validations) {
            notSupportedByList("objects");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final Json json = json(false, property);
            if(json != null){
                return json.toList();
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of objects, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of objects or {@code defaultValue}
     */
    private List<Object> objects(boolean validations, String property, List<Object> defaultValue) {
        List<Object> r = null;
        try {
            r = objects(validations, property);
        } catch (Exception ex) {
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns {@code true} if the value of the specified property is a list of objects, otherwise {@code false}
     *
     * @param property name of the property
     * @return {@code true} if the value of the specified property is a list of objects
     */
    public boolean isList(String property) {
        return isList(true, property);
    }

    /**
     * Returns {@code true} if the value of the specified property is a list of objects, otherwise {@code false}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return {@code true} if the value of the specified property is a list of objects
     */
    private boolean isList(boolean validations, String property) {
        try {
            if (validations) {
                notSupportedByList("is list property");
                notBlankPropertyName(property);
            }

            final List<Object> list = objects(false, property);
            return list != null;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Returns {@code true} if the value of the specified property is a map, otherwise {@code false}
     *
     * @param property name of the property
     * @return {@code true} if the value of the specified property is a map
     */
    public boolean isMap(String property) {
        return isMap(true, property);
    }

    /**
     * Returns {@code true} if the value of the specified property is a map, otherwise {@code false}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return {@code true} if the value of the specified property is a map
     */
    private boolean isMap(boolean validations, String property) {
        try {
            if (validations) {
                notSupportedByList("is map property");
                notBlankPropertyName(property);
            }

            final Json aMap = json(false, property);
            return aMap != null;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Returns the value to which the specified property is mapped if it is a map of objects, otherwise {@code null}
     *
     * @param property name of the property
     * @return the map of objects or null
     */
    public Map objectsMap(String property) {
        return objectsMap(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a map of objects, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the map of objects or {@code defaultValue}
     */
    public Map objectsMap(String property, Map defaultValue) {
        return objectsMap(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a map of objects, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the map of objects or null
     */
    private Map objectsMap(boolean validations, String property) {
        if(validations) {
            notSupportedByList("object maps");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final Json json = json(false, property);
            if(json != null){
                return json.objectsMap();
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a map of objects, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the map of objects or {@code defaultValue}
     */
    private Map objectsMap(boolean validations, String property, Map defaultValue) {
        Map r = null;
        try {
            r = objectsMap(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of maps of objects, otherwise
     * {@code null}
     *
     * @param property name of the property
     * @return the list of maps of objects or null
     */
    public List<Map> objectsMaps(String property) {
        return objectsMaps(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of maps of objects, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of maps of objects or {@code defaultValue}
     */
    public List<Map> objectsMaps(String property, List<Map> defaultValue) {
        return objectsMaps(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of maps of objects, otherwise
     * {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the list of maps of objects or null
     */
    private List<Map> objectsMaps(boolean validations, String property) {
        if(validations) {
            notSupportedByList("object maps");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final List<Object> objects = objects(false, property);
            if(objects != null){
                final List<Map> list = new ArrayList<>();
                for (Object object : objects) {
                    list.add(fromObject(object, false).toMap());
                }
                return list;
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of maps of objects, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of maps of objects or {@code defaultValue}
     */
    private List<Map> objectsMaps(boolean validations, String property, List<Map> defaultValue) {
        List<Map> r = null;
        try {
            r = objectsMaps(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of lists of objects, otherwise
     * {@code null}
     *
     * @param property name of the property
     * @return the list of lists of objects or null
     */
    public List<List> lists(String property) {
        return lists(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of lists of objects, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of lists of objects or {@code defaultValue}
     */
    public List<List> lists(String property, List<List> defaultValue) {
        return lists(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of lists of objects, otherwise
     * {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the list of lists of objects or null
     */
    private List<List> lists(boolean validations, String property) {
        if(validations) {
            notSupportedByList("lists");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final List<Object> objects = objects(false, property);
            if(objects != null){
                final List<List> list = new ArrayList<>();
                for (Object object : objects) {
                    list.add(fromObject(object, false).toList());
                }
                return list;
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of lists of objects, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of lists of objects or {@code defaultValue}
     */
    private List<List> lists(boolean validations, String property, List<List> defaultValue) {
        List<List> r = null;
        try {
            r = lists(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of jsons, otherwise {@code null}
     *
     * @param property name of the property
     * @return the list of jsons or null
     */
    public List<Json> jsons(String property) {
        return jsons(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of jsons, otherwise {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of jsons or {@code defaultValue}
     */
    public List<Json> jsons(String property, List<Json> defaultValue) {
        return jsons(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of jsons, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the list of jsons or null
     */
    private List<Json> jsons(boolean validations, String property) {
        if(validations) {
            notSupportedByList("jsons");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final List<Object> objects = objects(false, property);
            if(objects != null){
                final List<Json> list = new ArrayList<>();
                for (Object object : objects) {
                    list.add(fromObject(object, false));
                }
                return list;
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of jsons, otherwise {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of jsons or {@code defaultValue}
     */
    private List<Json> jsons(boolean validations, String property, List<Json> defaultValue) {
        List<Json> r = null;
        try {
            r = jsons(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped, otherwise {@code null}
     *
     * @param property name of the property
     * @return the string or null
     */
    public String string(String property) {
        return string(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped, otherwise {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the string or {@code defaultValue}
     */
    public String string(String property, String defaultValue) {
        return string(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the string or null
     */
    private String string(boolean validations, String property) {
        if(validations) {
            notSupportedByList("string");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final Object object = object(false, property);
            if(object != null){
                return object.toString();
            } else {
                return null;
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped, otherwise {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the string or {@code defaultValue}
     */
    private String string(boolean validations, String property, String defaultValue) {
        String r = null;
        try {
            r = string(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of strings, otherwise {@code null}
     *
     * @param property name of the property
     * @return the list of strings or null
     */
    public List<String> strings(String property) {
        return strings(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of strings, otherwise {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of strings or {@code defaultValue}
     */
    public List<String> strings(String property, List<String> defaultValue) {
        return strings(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of strings, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the list of strings or null
     */
    private List<String> strings(boolean validations, String property) {
        if(validations) {
            notSupportedByList("strings");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final List<Object> objects = objects(false, property);
            if(objects != null){
                final List<String> list = new ArrayList<>();
                for (Object object : objects) {
                    if (object != null) {
                        list.add(object.toString());
                    }
                }
                return list;
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of strings, otherwise {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of strings or {@code defaultValue}
     */
    private List<String> strings(boolean validations, String property, List<String> defaultValue) {
        List<String> r = null;
        try {
            r = strings(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a boolean, otherwise {@code null}
     *
     * @param property name of the property
     * @return the boolean or null
     */
    public Boolean bool(String property) {
        return bool(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a boolean, otherwise {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the boolean or {@code defaultValue}
     */
    public boolean bool(String property, boolean defaultValue) {
        return bool(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a boolean, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the boolean or null
     */
    private Boolean bool(boolean validations, String property) {
        if(validations) {
            notSupportedByList("bool");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final Object object = object(false, property);
            return convertToBoolean(object);
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a boolean, otherwise {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the boolean or {@code defaultValue}
     */
    private boolean bool(boolean validations, String property, boolean defaultValue) {
        Boolean r = null;
        try {
            r = bool(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns {@code true} if the the value of the property is {@link Boolean#TRUE}
     *
     * @param property name of the property
     * @return @code true} if the the value of the property is {@link Boolean#TRUE}
     */
    public boolean is(String property){
        return Boolean.TRUE.equals(bool(property));
    }

    /**
     * Returns {@code true} if the the value of the property is {@link Boolean#TRUE}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return @code true} if the the value of the property is {@link Boolean#TRUE}
     */
    public boolean is(String property, boolean defaultValue){
        return Boolean.TRUE.equals(bool(property, defaultValue));
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of booleans, otherwise {@code null}
     *
     * @param property name of the property
     * @return the list of booleans or null
     */
    public List<Boolean> bools(String property) {
        return bools(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of booleans, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of booleans or {@code defaultValue}
     */
    public List<Boolean> bools(String property, List<Boolean> defaultValue) {
        return bools(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of booleans, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the list of booleans or null
     */
    private List<Boolean> bools(boolean validations, String property) {
        if(validations) {
            notSupportedByList("bools");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final List<Object> objects = objects(false, property);
            if(objects != null){
                final List<Boolean> list = new ArrayList<>();
                for (Object object : objects) {
                    final Boolean b = convertToBoolean(object);
                    if (b != null) {
                        list.add(b);
                    }
                }
                return list;
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of booleans, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of booleans or {@code defaultValue}
     */
    private List<Boolean> bools(boolean validations, String property, List<Boolean> defaultValue) {
        List<Boolean> r = null;
        try {
            r = bools(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is an integer, otherwise {@code null}
     *
     * @param property name of the property
     * @return the integer or null
     */
    public Integer integer(String property) {
        return integer(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is an integer, otherwise {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the integer or {@code defaultValue}
     */
    public int integer(String property, int defaultValue) {
        return integer(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is an integer, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the integer or null
     */
    private Integer integer(boolean validations, String property) {
        if(validations) {
            notSupportedByList("integer");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final Object object = object(false, property);
            return convertToInteger(object);
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is an integer, otherwise {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the integer or {@code defaultValue}
     */
    private int integer(boolean validations, String property, int defaultValue) {
        Integer r = null;
        try {
            r = integer(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of integers, otherwise {@code null}
     *
     * @param property name of the property
     * @return the list of integers or null
     */
    public List<Integer> integers(String property) {
        return integers(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of integers, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of integers or {@code defaultValue}
     */
    public List<Integer> integers(String property, List<Integer> defaultValue) {
        return integers(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of integers, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the list of integers or null
     */
    private List<Integer> integers(boolean validations, String property) {
        if(validations) {
            notSupportedByList("integers");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final List<Object> objects = objects(false, property);
            if(objects != null){
                final List<Integer> list = new ArrayList<>();
                for (Object object : objects) {
                    final Integer i = convertToInteger(object);
                    if (i != null) {
                        list.add(i);
                    }
                }
                return list;
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of integers, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of integers or {@code defaultValue}
     */
    private List<Integer> integers(boolean validations, String property, List<Integer> defaultValue) {
        List<Integer> r = null;
        try {
            r = integers(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a long integer, otherwise {@code null}
     *
     * @param property name of the property
     * @return the long integer or null
     */
    public Long longInteger(String property) {
        return longInteger(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a long integer, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the long integer or {@code defaultValue}
     */
    public long longInteger(String property, long defaultValue) {
        return longInteger(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a long integer, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the long integer or null
     */
    private Long longInteger(boolean validations, String property) {
        if(validations) {
            notSupportedByList("long integer");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final Object object = object(false, property);
            return convertToLong(object);
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a long integer, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the long integer or {@code defaultValue}
     */
    private long longInteger(boolean validations, String property, long defaultValue) {
        Long r = null;
        try {
            r = longInteger(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of long integers, otherwise
     * {@code null}
     *
     * @param property name of the property
     * @return the list of long integers or null
     */
    public List<Long> longIntegers(String property) {
        return longIntegers(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of long integers, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of long integers or {@code defaultValue}
     */
    public List<Long> longIntegers(String property, List<Long> defaultValue) {
        return longIntegers(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of long integers, otherwise
     * {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the list of long integers or null
     */
    private List<Long> longIntegers(boolean validations, String property) {
        if(validations) {
            notSupportedByList("long integers");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final List<Object> objects = objects(false, property);
            if(objects != null){
                final List<Long> list = new ArrayList<>();
                for (Object object : objects) {
                    final Long li = convertToLong(object);
                    if (li != null) {
                        list.add(li);
                    }
                }
                return list;
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of long integers, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of long integers or {@code defaultValue}
     */
    private List<Long> longIntegers(boolean validations, String property, List<Long> defaultValue) {
        List<Long> r = null;
        try {
            r = longIntegers(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a double, otherwise {@code null}
     *
     * @param property name of the property
     * @return the double or null
     */
    public Double decimal(String property) {
        return decimal(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a double, otherwise {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the double or {@code defaultValue}
     */
    public double decimal(String property, double defaultValue) {
        return decimal(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a double, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the double or null
     */
    private Double decimal(boolean validations, String property) {
        if(validations) {
            notSupportedByList("decimal");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final Object object = object(false, property);
            return convertToDouble(object);
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a double, otherwise {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the double or {@code defaultValue}
     */
    private Double decimal(boolean validations, String property, double defaultValue) {
        Double r = null;
        try {
            r = decimal(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of doubles, otherwise {@code null}
     *
     * @param property name of the property
     * @return the list of doubles or null
     */
    public List<Double> decimals(String property) {
        return decimals(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of doubles, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of doubles or {@code defaultValue}
     */
    public List<Double> decimals(String property, List<Double> defaultValue) {
        return decimals(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of doubles, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the list of doubles or null
     */
    private List<Double> decimals(boolean validations, String property) {
        if(validations) {
            notSupportedByList("decimals");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final List<Object> objects = objects(false, property);
            if(objects != null){
                final List<Double> list = new ArrayList<>();
                for (Object object : objects) {
                    final Double d = convertToDouble(object);
                    if (d != null) {
                        list.add(d);
                    }
                }
                return list;
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of doubles, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of doubles or {@code defaultValue}
     */
    private List<Double> decimals(boolean validations, String property, List<Double> defaultValue) {
        List<Double> r = null;
        try {
            r = decimals(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a date, otherwise {@code null}
     *
     * @param property name of the property
     * @return the date or null
     */
    public Date date(String property) {
        return date(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a date, otherwise {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the date or {@code defaultValue}
     */
    public Date date(String property, Date defaultValue) {
        return date(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a date, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the date or null
     */
    private Date date(boolean validations, String property) {
        if(validations) {
            notSupportedByList("date");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final Object object = object(false, property);
            return convertToDate(object);
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a date, otherwise {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the date or {@code defaultValue}
     */
    private Date date(boolean validations, String property, Date defaultValue) {
        Date r = null;
        try {
            r = date(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of dates, otherwise {@code null}
     *
     * @param property name of the property
     * @return the list of dates or null
     */
    public List<Date> dates(String property) {
        return dates(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of dates, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of dates or {@code defaultValue}
     */
    public List<Date> dates(String property, List<Date> defaultValue) {
        return dates(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of dates, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the list of dates or null
     */
    private List<Date> dates(boolean validations, String property) {
        if(validations) {
            notSupportedByList("dates");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final List<Object> objects = objects(false, property);
            if(objects != null){
                final List<Date> list = new ArrayList<>();
                for (Object object : objects) {
                    final Date d = convertToDate(object);
                    if (d != null) {
                        list.add(d);
                    }
                }
                return list;
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of dates, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of dates or {@code defaultValue}
     */
    private List<Date> dates(boolean validations, String property, List<Date> defaultValue) {
        List<Date> r = null;
        try {
            r = dates(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a big integer, otherwise {@code null}
     *
     * @param property name of the property
     * @return the big integer or null
     */
    public BigInteger bigInteger(String property) {
        return bigInteger(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a big integer, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the big integer or {@code defaultValue}
     */
    public BigInteger bigInteger(String property, BigInteger defaultValue) {
        return bigInteger(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a big integer, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the big integer or null
     */
    private BigInteger bigInteger(boolean validations, String property) {
        if(validations) {
            notSupportedByList("big integer");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final Object object = object(false, property);
            return convertToBigInteger(object);
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a big integer, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the big integer or {@code defaultValue}
     */
    private BigInteger bigInteger(boolean validations, String property, BigInteger defaultValue) {
        BigInteger r = null;
        try {
            r = bigInteger(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of big integers, otherwise
     * {@code null}
     *
     * @param property name of the property
     * @return the list of big integers or null
     */
    public List<BigInteger> bigIntegers(String property) {
        return bigIntegers(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of big integers, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of big integers or {@code defaultValue}
     */
    public List<BigInteger> bigIntegers(String property, List<BigInteger> defaultValue) {
        return bigIntegers(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of big integers, otherwise
     * {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the list of big integers or null
     */
    private List<BigInteger> bigIntegers(boolean validations, String property) {
        if(validations) {
            notSupportedByList("big integers");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final List<Object> objects = objects(false, property);
            if(objects != null){
                final List<BigInteger> list = new ArrayList<>();
                for (Object object : objects) {
                    final BigInteger bi = convertToBigInteger(object);
                    if (bi != null) {
                        list.add(bi);
                    }
                }
                return list;
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of big integers, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of big integers or {@code defaultValue}
     */
    private List<BigInteger> bigIntegers(boolean validations, String property, List<BigInteger> defaultValue) {
        List<BigInteger> r = null;
        try {
            r = bigIntegers(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a big decimal, otherwise {@code null}
     *
     * @param property name of the property
     * @return the big decimal or null
     */
    public BigDecimal bigDecimal(String property) {
        return bigDecimal(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a big decimal, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the big decimal or {@code defaultValue}
     */
    public BigDecimal bigDecimal(String property, BigDecimal defaultValue) {
        return bigDecimal(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a big decimal, otherwise {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the big decimal or null
     */
    private BigDecimal bigDecimal(boolean validations, String property) {
        if(validations) {
            notSupportedByList("big decimal");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final Object object = object(false, property);
            return convertToBigDecimal(object);
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a big decimal, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the big decimal or {@code defaultValue}
     */
    private BigDecimal bigDecimal(boolean validations, String property, BigDecimal defaultValue) {
        BigDecimal r = null;
        try {
            r = bigDecimal(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of big decimals, otherwise
     * {@code null}
     *
     * @param property name of the property
     * @return the list of big decimals or null
     */
    public List<BigDecimal> bigDecimals(String property) {
        return bigDecimals(true, property);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of big decimals, otherwise
     * {@code defaultValue}
     *
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of big decimals or {@code defaultValue}
     */
    public List<BigDecimal> bigDecimals(String property, List<BigDecimal> defaultValue) {
        return bigDecimals(true, property, defaultValue);
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of big decimals, otherwise
     * {@code null}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @return the list of big decimals or null
     */
    private List<BigDecimal> bigDecimals(boolean validations, String property) {
        if(validations) {
            notSupportedByList("big decimals");
            notBlankPropertyName(property);
        }

        if(!isEmpty(property)){
            final List<Object> objects = objects(false, property);
            if(objects != null){
                final List<BigDecimal> list = new ArrayList<>();
                for (Object object : objects) {
                    final BigDecimal bd = convertToBigDecimal(object);
                    if (bd != null) {
                        list.add(bd);
                    }
                }
                return list;
            }
        }
        return null;
    }

    /**
     * Returns the value to which the specified property is mapped if it is a list of big decimals, otherwise
     * {@code defaultValue}
     *
     * @param validations check support and given parameters
     * @param property name of the property
     * @param defaultValue default value
     * @return the list of big decimals or {@code defaultValue}
     */
    private List<BigDecimal> bigDecimals(boolean validations, String property, List<BigDecimal> defaultValue) {
        List<BigDecimal> r = null;
        try {
            r = bigDecimals(validations, property);
        } catch (Exception ex){
            logger.trace(String.format("Exception when process property [%s]: %s", property, ex.getMessage()));
        }
        return r != null ? r : defaultValue;
    }

    /**
     * Throws an exception if the json is a map
     *
     * @param operation operation name
     */
    private void notSupportedByMap(String operation){
        if (isMap()) {
            throw new IllegalStateException(String.format("Operation [%s] not supported for a map", operation));
        }
    }

    /**
     * Throws an exception if the json is a list
     *
     * @param operation operation name
     */
    private void notSupportedByList(String operation){
        if (isList()) {
            throw new IllegalStateException(String.format("Operation [%s] not supported for a list", operation));
        }
    }

    /**
     * Throws an exception if the property name is empty
     *
     * @param propertyName property name to check
     */
    private static void notBlankPropertyName(String propertyName){
        if (StringUtils.isBlank(propertyName)) {
            throw new IllegalArgumentException("Property name is empty");
        }
    }

    /**
     * Returns the object if it is not empty
     *
     * @param value object to check
     * @return the value if not empty, otherwise returns null
     */
    private static Object returnIfNotEmpty(Object value){
        Object notEmptyValue = null;
        if(value != null) {
            if (value instanceof Json) {
                if (((Json) value).isNotEmpty()) {
                    notEmptyValue = value;
                }
            } else if (value instanceof String) {
                if (StringUtils.isNotBlank((String) value)) {
                    notEmptyValue = value;
                }
            } else if (value instanceof Collection) {
                if (!((Collection) value).isEmpty()) {
                    notEmptyValue = value;
                }
            } else if (value instanceof Map) {
                if (!((Map) value).isEmpty()) {
                    notEmptyValue = value;
                }
            }
        }
        return notEmptyValue;
    }

    /**
     * Returns the object if it is a json
     *
     * @param item object to check
     * @return the json value, otherwise returns null
     */
    private static Json objectItemToJson(Object item){
        Json value = null;
        if (item != null) {
            if (item instanceof Json) {
                value = ((Json) item);
            } else if (Map.class.isAssignableFrom(item.getClass())) {
                value = fromMap((Map<String, Object>) item);
            } else if (List.class.isAssignableFrom(item.getClass())) {
                value = fromList((List<Object>) item);
            }
        }
        return value;
    }

    /**
     * Returns the object if it is a json
     *
     * @param item object to check
     * @return the json value, otherwise returns null
     */
    private static Object jsonItemToObject(Object item){
        Object value = item;
        if (item != null) {
            if (item instanceof Json) {
                value = ((Json) item).toObject();
            } else if (item instanceof List) {
                value = fromObject(item).toList();
            } else if (item instanceof Map) {
                value = fromObject(item).toMap();
            }
        }
        return value;
    }

    /**
     * Check if the property exists on the map
     *
     * @param map source of properties
     * @param property name of the property
     * @return true if the property value is empty or does not exist on map
     */
    private static boolean isEmptyProperty(Map<String, Object> map, String property) {
        if (map == null || StringUtils.isBlank(property)) {
            return true;
        }
        final Object value = map.get(property);
        return value == null || value instanceof String && StringUtils.isBlank((String) value);
    }

    /**
     * Converts the value to integer if is possible
     *
     * @param value value to convert
     * @return integer value or null
     */
    private static Integer convertToInteger(Object value) {
        if(value == null){
            return null;
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            return Integer.parseInt(value.toString());
        }
    }

    /**
     * Converts the value to long integer if is possible
     *
     * @param value value to convert
     * @return long integer value or null
     */
    private static Long convertToLong(Object value) {
        if(value == null){
            return null;
        } else if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else {
            return Long.parseLong(value.toString());
        }
    }

    /**
     * Converts the value to double if is possible
     *
     * @param value value to convert
     * @return double value or null
     */
    private static Double convertToDouble(Object value) {
        if(value == null){
            return null;
        } else if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else {
            return Double.parseDouble(value.toString());
        }
    }

    /**
     * Converts the value to boolean if is possible
     *
     * @param value value to convert
     * @return boolean value or null
     */
    private static Boolean convertToBoolean(Object value) {
        if(value == null){
            return null;
        } else if(value instanceof Boolean){
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue() != 0;
        } else {
            return Boolean.parseBoolean(value.toString());
        }
    }

    /**
     * Converts the value to date if is possible
     *
     * @param value value to convert
     * @return date value or null
     */
    private static Date convertToDate(Object value) {
        if(value == null){
            return null;
        } else if(value instanceof Date){
            return (Date) value;
        } else {
            Long lg;
            try {
                lg = convertToLong(value);
            } catch (Exception ex){
                lg = null;
            }
            if(lg != null) {
                return new Date(lg);
            } else {
                try {
                    return DateFormat.getDateInstance().parse(value.toString());
                } catch (Exception ex){
                    return null;
                }
            }
        }
    }

    /**
     * Converts the value to big integer if is possible
     *
     * @param value value to convert
     * @return big integer value or null
     */
    private static BigInteger convertToBigInteger(Object value) {
        if(value == null){
            return null;
        } else if(value instanceof BigInteger){
            return (BigInteger) value;
        } else if(value instanceof BigDecimal){
            return ((BigDecimal) value).toBigInteger();
        } else {
            Long lg;
            try {
                lg = convertToLong(value);
            } catch (Exception ex){
                lg = null;
            }
            if(lg != null) {
                return BigInteger.valueOf(lg);
            } else {
                return null;
            }
        }
    }

    /**
     * Converts the value to big decimal if is possible
     *
     * @param value value to convert
     * @return big decimal value or null
     */
    private static BigDecimal convertToBigDecimal(Object value) {
        if(value == null){
            return null;
        } else if(value instanceof BigDecimal){
            return (BigDecimal) value;
        } else if(value instanceof BigInteger){
            return new BigDecimal((BigInteger) value);
        } else {
            Double db;
            try {
                db = convertToDouble(value);
            } catch (Exception ex){
                db = null;
            }
            if(db != null) {
                return BigDecimal.valueOf(db);
            } else {
                return null;
            }
        }
    }

    /**
     * Returns a string representation of the json object.
     *
     * @return string representation of json object
     */
    @Override
    public String toString() {
        return objectToString(toObject());
    }

    /**
     * Returns a string representation of the json object using the pretty printer for indentation.
     *
     * @return string representation of json object
     */
    public String toPrettyString() {
        return objectToPrettyString(toObject());
    }

    /**
     * Method that can be used to serialize any Java value as a String.
     *
     * @param object object to serialize
     * @return string representation of object
     */
    public static String objectToString(Object object) {
        return objectToString(object, false);
    }

    /**
     * Method that can be used to serialize any Java value as a String using the pretty printer for indentation.
     *
     * @param object object to serialize
     * @return string representation of object
     */
    public static String objectToPrettyString(Object object) {
        return objectToString(object, true);
    }

    /**
     * Method that can be used to serialize any Java value as a String.
     *
     * @param object object to serialize
     * @param pretty true if the objects will be serialized using the pretty printer for indentation
     * @return string representation of object
     */
    private static String objectToString(Object object, boolean pretty) {
        if (object == null) {
            return "null";
        }
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule mod = new SimpleModule("JSON parser Module");
        mod.addSerializer(new CustomSerializer(Json.class));
        mod.addSerializer(new CustomFormSerializer());
        mapper.registerModule(mod);

        String result;
        try {
            if(pretty) {
                result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            } else {
                result = mapper.writeValueAsString(object);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not convert object to string", e);
        }
        return result;
    }

    /**
     * Serializer helper for {@code Json} objects
     */
    private static class CustomSerializer extends StdSerializer<Json> {
        public CustomSerializer(Class<Json> t) {
            super(t);
        }

        @Override
        public void serialize(Json json, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
            if(json.isMap()){
                final JsonSerializer<Object> listSerializer = provider.findValueSerializer(Map.class, null);
                listSerializer.serialize(json.toMap(), jsonGenerator, provider);
            } else if(json.isList()){
                final JsonSerializer<Object> listSerializer = provider.findValueSerializer(List.class, null);
                listSerializer.serialize(json.toList(), jsonGenerator, provider);
            } else {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeEndObject();
            }
        }
    }

    /**
     * Serializer helper for {@code Form} objects
     */
    private static class CustomFormSerializer extends StdSerializer<Form> {
        public CustomFormSerializer() {
            super(Form.class);
        }

        @Override
        public void serialize(Form form, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
            Object obj = form.asMap();
            if(obj != null){
                final JsonSerializer<Object> listSerializer = provider.findValueSerializer(Map.class, null);
                listSerializer.serialize(obj, jsonGenerator, provider);
            } else {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeEndObject();
            }
        }
    }

    /**
     * Indicates if string represents the same json object than this one.
     *
     * @param jsonString string to compare
     * @return true if string represents the same json object than this one.
     */
    public boolean equals(String jsonString) {
        return StringUtils.isNotBlank(jsonString) && equals((Object) jsonString);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        try {
            if (!Json.class.equals(o.getClass())){
                o = fromObject(o.toString());
                if (o == null){
                    return false;
                }
            }

            final Json json = (Json) o;
            if(isMap() != json.isMap()){
                return false;
            }

            if(isList()){
                // compare lists
                if(list.isEmpty()){
                    return json.list.isEmpty();
                } else if(json.list.isEmpty()){
                    return false;
                }
                if(list.size() != json.list.size()){
                    return false;
                }
                if (!new HashSet<>(list).containsAll(json.list) || !new HashSet<>(json.list).containsAll(list)) {
                    return false;
                }
                for (int i = 0; i < list.size(); i++) {
                    if(list.get(0) != null && !list.get(0).equals(json.list.get(0))){
                        return false;
                    }
                }

                return true;
            } else {
                // compare maps
                if(map.isEmpty()){
                    return json.map.isEmpty();
                } else if(json.map.isEmpty()){
                    return false;
                }
                if(map.size() != json.map.size()){
                    return false;
                }
                if (!map.keySet().containsAll(json.map.keySet()) || !json.map.keySet().containsAll(map.keySet())) {
                    return false;
                }
                // both maps contains the same keys, compare internal objects
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    Object ov = json.map.get(entry.getKey());
                    if(entry.getValue() instanceof Json){
                        if(! entry.getValue().equals(fromObject(ov))){
                            return false;
                        }
                    } else if(entry.getValue() instanceof Map){
                        if(! fromMap((Map) entry.getValue()).equals(fromObject(ov))){
                            return false;
                        }
                    } else if(entry.getValue() instanceof List){
                        if(! fromList((List) entry.getValue()).equals(fromObject(ov))){
                            return false;
                        }
                    } else {
                        if(entry.getValue() instanceof Number && ov instanceof Number){
                            if(compareNumbers((Number)entry.getValue(), (Number)ov) != 0){
                                return false;
                            }
                        } else if(!entry.getValue().equals(ov)){
                            return false;
                        }
                    }
                }

                return true;
            }
        } catch (Exception ex){
            return false;
        }
    }

    /**
     * Compares two Number objects
     */
    private int compareNumbers(final Number x, final Number y) {
        if(isSpecialNumber(x) || isSpecialNumber(y))
            return Double.compare(x.doubleValue(), y.doubleValue());
        else
            return toBigDecimal(x).compareTo(toBigDecimal(y));
    }

    /**
     * True if the Number object is an special number type (NaN / infinite)
     */
    private static boolean isSpecialNumber(final Number number) {
        boolean specialDouble = number instanceof Double && (Double.isNaN((Double) number) || Double.isInfinite((Double) number));
        boolean specialFloat = number instanceof Float && (Float.isNaN((Float) number) || Float.isInfinite((Float) number));
        return specialDouble || specialFloat;
    }

    /**
     * Converts to a big decimal object
     */
    private static BigDecimal toBigDecimal(final Number number) {
        if(number instanceof BigDecimal)
            return (BigDecimal) number;
        if(number instanceof BigInteger)
            return new BigDecimal((BigInteger) number);
        if(number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long)
            return new BigDecimal(number.longValue());
        if(number instanceof Float || number instanceof Double)
            return BigDecimal.valueOf(number.doubleValue());

        try {
            return new BigDecimal(number.toString());
        } catch(final NumberFormatException e) {
            throw new RuntimeException(
                    String.format("The given number [%s] of class [%s] does not have a parsable string representation",
                            number,
                            number.getClass().getName())
                    , e);
        }
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = result + 31 * (map != null ? map.hashCode() : 0);
        result = result + 47 * (list != null ? list.hashCode() : 0);
        return result;
    }

    /**
     * Merge the values of the given json with the current one. The internal Json, maps and list objects are merged too.
     *
     * @param json json to merge
     */
    public void merge(Json json) {
        merge(json, true);
    }

    /**
     * Merge the values of the given json with the current one.
     *
     * @param json json to merge
     * @param mergeInternalValues if true, the internal Json, maps and list objects are merged too. Otherwise, the Json,
     *                           maps and list are replaced by the new ones.
     */
    public void merge(Json json, boolean mergeInternalValues) {
        if(json == null){
            return;
        }
        if(isMap() != json.isMap()){
            throw new IllegalArgumentException("You cannot merge a list with a map");
        }

        if(isList()){
            // merge lists
            if(mergeInternalValues){
                json.streamList()
                        .filter(o -> !containsObject(o))
                        .forEach(list::add);
            } else {
                list.addAll(json.objects());
            }
        } else {
            // merge maps
            if(mergeInternalValues) {
                json.forEachMap((key, value) -> {
                    if (value instanceof Json || value instanceof Map || value instanceof List) {
                        final Json jsonValue = fromObject(value);

                        boolean saved = false;

                        final Object originalValue = map.get(key);
                        if (originalValue instanceof Json || originalValue instanceof Map || originalValue instanceof List) {
                            final Json originalJsonValue = fromObject(originalValue);
                            if (jsonValue.isMap() == originalJsonValue.isMap()) {
                                // is both are the same kind, merge them
                                try {
                                    originalJsonValue.merge(jsonValue, true);
                                    map.put(key, originalJsonValue);
                                    saved = true;
                                } catch (Exception ex) {
                                    saved = false;
                                }
                            }
                        }

                        if (!saved) {
                            if (jsonValue.isMap()) {
                                map.put(key, jsonValue.toMap());
                            } else if (jsonValue.isList()) {
                                map.put(key, jsonValue.toList());
                            } else {
                                map.remove(key);
                            }
                        }
                    } else {
                        map.put(key, value);
                    }
                });
            } else {
                json.forEachMap((key, value) -> {
                    if (value instanceof Json || value instanceof Map || value instanceof List) {
                        map.put(key, fromObject(value));
                    } else {
                        map.put(key, value);
                    }
                });
            }
        }
    }

    public static class Visitor {
        public Object convertValue(String key, Object value, String path) {
            return value;
        }

        public void enrich(String path, Json json) {}

        public String convertKey(String key, String path) {
            return key;
        }
    }

    public void traverse(Visitor visitor) {
        if (isMap()) {
            visitor.enrich("", this);
        }
        traverseRecursive(visitor, "", this);
    }

    private void traverseRecursive(Visitor visitor, String path, Json currentLevel) {
        if (currentLevel.isMap()) {
            Map<String, Object> originalLevel = currentLevel.map;
            Map<String, Object> newLevel = new LinkedHashMap<>();
            for (String key : originalLevel.keySet()) {
                Object value = originalLevel.get(key);
                Object newValue;
                if (value instanceof Json || value instanceof Map || value instanceof List) {
                    Json nested = fromObject(value);
                    if (nested.isMap()) {
                        visitor.enrich(buildPath(path, key), nested);
                    }
                    traverseRecursive(visitor, buildPath(path, key), nested);
                    newValue = visitor.convertValue(key, nested, buildPath(path, key));
                } else {
                    newValue = visitor.convertValue(key, value, buildPath(path, key));
                }
                newLevel.put(visitor.convertKey(key, path), newValue);
            }
            currentLevel.map.clear();
            currentLevel.map.putAll(newLevel);
        } else {
            List<Object> originalList = currentLevel.list;
            List<Object> newLevel = new ArrayList<>();
            ListIterator<Object> it = originalList.listIterator();
            int index = 0;
            while (it.hasNext()) {
                Object item = it.next();
                Object newValue;
                if (item instanceof Json || item instanceof Map || item instanceof List) {
                    Json nested = fromObject(item);
                    if (nested.isMap()) {
                        visitor.enrich(buildPath(path, index), nested);
                    }
                    traverseRecursive(visitor, buildPath(path, index), nested);
                    newValue = visitor.convertValue(buildPath(lastPathPart(path), index), nested, buildPath(path, index));
                } else {
                    newValue = visitor.convertValue(lastPathPart(path), item, buildPath(path, index));
                }
                newLevel.add(newValue);
                index++;
            }
            currentLevel.list.clear();
            currentLevel.list.addAll(newLevel);
        }
    }

    private String buildPath(String prefix, String fieldName) {
        if (StringUtils.isBlank(prefix)) {
            return fieldName;
        } else {
            return prefix + "." + fieldName;
        }
    }

    private String buildPath(String base, int index) {
        return base + "[" + index + "]";
    }

    private String buildPath(String prefix, String fieldName, int index) {
        String base = buildPath(prefix, fieldName);
        return base + "[" + index + "]";
    }

    private String lastPathPart(String path) {
        if (StringUtils.isBlank(path)) {
            return "";
        }
        String[] parts = StringUtils.split(path, ".");
        return parts[parts.length - 1];
    }

    /**
     * Returns the same object. Implemented to support the JsonMapSource interface.
     *
     * @return json object
     */
    @Override
    public Json toJson() {
        return this;
    }
}