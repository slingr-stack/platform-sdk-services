package io.slingr.services.configurations.sources;

import io.slingr.services.utils.FilesUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Implementation of {@link PropertySource} that support properties files as source of service properties
 *
 * <p>Created by lefunes on 22/03/18.
 */
public class PropertiesFileSource implements PropertySource {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesFileSource.class);

    private final String filename;
    private final Properties properties = new Properties();

    /**
     * Construct a properties file source
     *
     * @param propertyFilename name of the properties filename to use as source
     * @param internal true if the file will be read from classpath, false if want to load the file from local environment
     */
    public PropertiesFileSource(String propertyFilename, boolean internal) {
        this.filename = propertyFilename;
        if(StringUtils.isBlank(propertyFilename)){
            logger.error(String.format("Invalid name of property filename. This source will be ignored: %s", propertyFilename));
        } else {
            InputStream input = null;
            try {
                if(internal) {
                    input = FilesUtils.getInternalFile(propertyFilename);
                } else {
                    input = FilesUtils.getLocalFile(propertyFilename);
                }

                // load a properties file
                properties.load(input);
            } catch (Exception ex) {
                logger.error(String.format("Exception when read property file [%s]. This source will be ignored: %s", propertyFilename, ex));
            } finally {
                if (input != null) {
                    try {
                        input.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public String findProperty(String propertyName) {
        if(!properties.isEmpty() && StringUtils.isNotBlank(propertyName) && properties.containsKey(propertyName)) {
            try {
                return properties.getProperty(propertyName);
            } catch (Exception ex){
                try {
                    logger.warn(String.format("Property [%s] on file [%s] is invalid and will be ignored [%s]: %s", propertyName, filename, properties.get(propertyName), ex));
                } catch (Exception ex2){
                    logger.warn(String.format("Property [%s] on file [%s] is invalid and will be ignored: %s (%s)", propertyName, filename, ex, ex2));
                }
            }
        }
        return null;
    }
}
