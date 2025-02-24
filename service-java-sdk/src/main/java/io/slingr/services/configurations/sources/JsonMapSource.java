package io.slingr.services.configurations.sources;

import io.slingr.services.utils.Json;
import io.slingr.services.utils.converters.JsonSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link PropertySource} that support Json as source of service properties
 *
 * <p>Created by lefunes on 22/03/18.
 */
public class JsonMapSource implements PropertySource {
    private static final Logger logger = LoggerFactory.getLogger(JsonMapSource.class);

    private final Json json;

    /**
     * Construct a Json source
     *
     * @param jsonSource json map source to use as source
     */
    public JsonMapSource(JsonSource jsonSource) {
        if(jsonSource == null){
            logger.error("Invalid Json to use as source. This source will be ignored");
            jsonSource = Json.map();
        }
        Json json = jsonSource.toJson();
        if(json == null || !json.isMap()){
            logger.error(String.format("Invalid Json to use as source. This source will be ignored: %s", json));
            json = Json.map();
        }
        this.json = json;
    }

    @Override
    public String findProperty(String propertyName) {
        if(StringUtils.isNotBlank(propertyName) && json.contains(propertyName)) {
            try {
                return json.string(propertyName);
            } catch (Exception ex){
                logger.warn(String.format("Property [%s] on json is invalid and will be ignored [%s]: %s", propertyName, json.object(propertyName), ex.getMessage()));
            }
        }
        return null;
    }
}