package io.slingr.services.configurations.sources;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link PropertySource} that support environment variables as source of service properties
 *
 * <p>Created by lefunes on 22/03/18.
 */
public class EnvVarsSource implements PropertySource {
    private static final Logger logger = LoggerFactory.getLogger(EnvVarsSource.class);

    @Override
    public String findProperty(String propertyName) {
        if(StringUtils.isNotBlank(propertyName)) {
            try {
                return System.getenv(propertyName);
            } catch (Exception ex){
                logger.warn(String.format("Property [%s] on json is environment variables and will be ignored: %s", propertyName, ex.getMessage()));
            }
        }
        return null;
    }
}
