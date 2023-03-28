package io.slingr.services.framework;

import io.slingr.services.Service;
import org.apache.commons.lang3.StringUtils;

/**
 * Class used to initialize and run the service.
 *
 * <p>Created by lefunes on 22/06/2018
 */
public abstract class IRunner {

    /**
     * Starts the service with the environment information.
     */
    public final Service startService() throws Exception {
        return startService((String) null);
    }

    /**
     * Starts the service with the environment information.
     *
     * @param configurationFile property file used as source of properties. This is used primary on development time.
     */
    public final Service startService(final String configurationFile) throws Exception {
        return startService(configurationFile, null);
    }

    /**
     * Starts the service with the environment information.
     *
     * @param configurationFile property file used as source of properties. This is used primary on development time.
     * @param defaultWebServiceUri defaults web services uri.
     */
    public final Service startService(final String configurationFile, final String defaultWebServiceUri) throws Exception {
        final Service service = createService();
        startService(service, configurationFile, defaultWebServiceUri);
        return service;
    }

    /**
     * Starts the service with the environment information.
     *
     * @param service service to initialize.
     */
    public final Service startService(final Service service) throws Exception {
        return startService(service, null);
    }

    /**
     * Starts the service with the environment information.
     *
     * @param service service to initialize.
     * @param configurationFile property file used as source of properties. This is used primary on development time.
     */
    public final Service startService(final Service service, final String configurationFile) throws Exception {
        return startService(service, configurationFile, null);
    }

    /**
     * Starts the service with the environment information.
     *
     * @param service service to initialize.
     * @param configurationFile property file used as source of properties. This is used primary on development time.
     * @param defaultWebServiceUri defaults web services uri.
     */
    public abstract Service startService(final Service service, final String configurationFile, final String defaultWebServiceUri) throws Exception;

    /**
     * Create a new service instance
     */
    public abstract Service createService();

    /**
     * Extract the parameter value from the command line arguments.
     *
     * @param key parameter key.
     * @param args command line arguments.
     * @return parameter value.
     */
    public static String extractArgument(final String key, final String[] args) {
        String value = null;
        if(args != null && StringUtils.isNotBlank(key)) {
            final String toFind = key + "=";
            if(args != null && args.length > 0) {
                for(String arg : args) {
                    if(arg.startsWith(toFind)) {
                        final String[] parts = arg.split("=");
                        if(parts.length > 1 && StringUtils.isNotBlank(parts[1])) {
                            value = parts[1];
                            break;
                        }
                    }
                }
            }
        }
        return value;
    }
}
