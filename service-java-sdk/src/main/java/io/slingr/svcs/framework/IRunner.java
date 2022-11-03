package io.slingr.svcs.framework;

import io.slingr.svcs.Svc;
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
    public final Svc startSvc() throws Exception {
        return startSvc((String) null);
    }

    /**
     * Starts the service with the environment information.
     *
     * @param configurationFile property file used as source of properties. This is used primary on development time.
     */
    public final Svc startSvc(final String configurationFile) throws Exception {
        return startSvc(configurationFile, null);
    }

    /**
     * Starts the service with the environment information.
     *
     * @param configurationFile property file used as source of properties. This is used primary on development time.
     * @param defaultWebServiceUri defaults web services uri.
     */
    public final Svc startSvc(final String configurationFile, final String defaultWebServiceUri) throws Exception {
        final Svc svc = createSvc();
        startSvc(svc, configurationFile, defaultWebServiceUri);
        return svc;
    }

    /**
     * Starts the service with the environment information.
     *
     * @param svc service to initialize.
     */
    public final Svc startSvc(final Svc svc) throws Exception {
        return startSvc(svc, null);
    }

    /**
     * Starts the service with the environment information.
     *
     * @param svc service to initialize.
     * @param configurationFile property file used as source of properties. This is used primary on development time.
     */
    public final Svc startSvc(final Svc svc, final String configurationFile) throws Exception {
        return startSvc(svc, configurationFile, null);
    }

    /**
     * Starts the service with the environment information.
     *
     * @param svc service to initialize.
     * @param configurationFile property file used as source of properties. This is used primary on development time.
     * @param defaultWebServiceUri defaults web services uri.
     */
    public abstract Svc startSvc(final Svc svc, final String configurationFile, final String defaultWebServiceUri) throws Exception;

    /**
     * Create a new service instance
     */
    public abstract Svc createSvc();

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
