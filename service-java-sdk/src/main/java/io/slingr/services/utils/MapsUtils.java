package io.slingr.services.utils;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utils to perform operation with maps
 *
 */
public class MapsUtils {

    public static Map<String, Object> cleanDotKeys(Map<String, Object> map){
        final Map<String, Object> result = new HashMap<>();
        if(map != null && !map.isEmpty()){
            for (String key : map.keySet()){
                final String newKey = cleanDotKey(key);
                result.put(newKey, cleanDotKeys(map.get(key)));
            }
        }
        return result;
    }

    public static Object cleanDotKeys(Object object){
        if(object instanceof Json){
            if(((Json) object).isList()){
                return cleanDotKeys(((Json) object).toList());
            } else if(((Json) object).isMap()){
                return cleanDotKeys(((Json) object).toMap());
            } else {
                return null;
            }
        } else if(object instanceof Map){
            return cleanDotKeys((Map)object);
        } else if(object instanceof List){
            final List list = new ArrayList();
            for (Object o : (List) object) {
                list.add(cleanDotKeys(o));
            }
            return list;
        } else {
            return object;
        }
    }

    public static String cleanDotKey(String key){
        if(StringUtils.isBlank(key)){
            return "";
        } else {
            return key.replaceAll("\\.", "_").trim();
        }
    }
}
