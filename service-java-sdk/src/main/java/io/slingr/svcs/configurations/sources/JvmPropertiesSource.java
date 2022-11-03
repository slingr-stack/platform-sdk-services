package io.slingr.svcs.configurations.sources;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link PropertySource} that support the properties defined on the JVM as source of service
 * properties
 *
 * <p>Created by lefunes on 22/03/18.
 */
public class JvmPropertiesSource implements PropertySource {
    private static final Logger logger = LoggerFactory.getLogger(JvmPropertiesSource.class);

    @Override
    public String findProperty(String propertyName) {
        if(StringUtils.isNotBlank(propertyName)) {
            try {
                return System.getProperty(propertyName);
            } catch (Exception ex){
                logger.warn(String.format("Property [%s] on JVM properties is invalid and will be ignored: %s", propertyName, ex.getMessage()));
            }
        }
        return null;
    }
}
