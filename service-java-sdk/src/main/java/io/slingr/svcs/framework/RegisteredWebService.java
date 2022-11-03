package io.slingr.svcs.framework;

import io.slingr.svcs.framework.annotations.classes.ClassWebService;
import io.slingr.svcs.framework.annotations.classes.MethodAccessorType;
import io.slingr.svcs.framework.annotations.classes.MethodParameterType;
import io.slingr.svcs.framework.annotations.classes.WebServiceResponseType;
import io.slingr.svcs.services.rest.RestMethod;
import io.slingr.svcs.utils.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple object to keep the information about a service web service
 *
 * <p>Created by lefunes on 14/11/16.
 */
public class RegisteredWebService implements Comparable<RegisteredWebService> {
    private static final Logger logger = LoggerFactory.getLogger(RegisteredWebService.class);

    private final String key;
    private final String name;
    private final RestMethod restMethod;
    private final String path;
    private final String method;
    private final MethodParameterType parameterType;
    private final WebServiceResponseType responseType;
    private final MethodAccessorType accessorType;
    private final Map<String, Integer> variables = new HashMap<>();
    private final Pattern pattern;
    private final Pattern extractPattern;
    private final Class<?> svcClass;

    private static Method namedGroupsMethod = null;
    static {
        try {
            namedGroupsMethod = Pattern.class.getDeclaredMethod("namedGroups");
            namedGroupsMethod.setAccessible(true);
        } catch (Exception ex){
            logger.error(String.format("Svc can not extract variables on web services path: %s", ex.getMessage()), ex);
            namedGroupsMethod = null;
        }
    }

    /**
     * Instances a function declared on the service
     *
     * @param name name of the web service
     * @param restMethod HTTP method of the web service
     * @param path path of the web service
     * @param method name of the java method
     * @param parameterType type of the parameter declared on java method
     * @param responseType type of the response of the java method
     * @param accessorType type of the accessor of the java method
     */
    public RegisteredWebService(String name, RestMethod restMethod, String path, String method, MethodParameterType parameterType, WebServiceResponseType responseType, MethodAccessorType accessorType) {
        this(name, restMethod, path, method, parameterType, responseType, accessorType, null);
    }

    /**
     * Instances a function declared on the service
     *
     * @param name name of the web service
     * @param restMethod HTTP method of the web service
     * @param path path of the web service
     * @param method name of the java method
     * @param parameterType type of the parameter declared on java method
     * @param responseType type of the response of the java method
     * @param accessorType type of the accessor of the java method
     * @param svcClass class that implements the method
     */
    public RegisteredWebService(String name, RestMethod restMethod, String path, String method, MethodParameterType parameterType, WebServiceResponseType responseType, MethodAccessorType accessorType, Class<?> svcClass) {
        this.name = name;
        this.restMethod = restMethod;
        this.path = path;
        this.method = method;
        this.parameterType = parameterType;
        this.responseType = responseType;
        this.accessorType = accessorType;
        this.svcClass = svcClass;

        final String cleanedPath = ClassWebService.cleanPath(path);
        final String regex = cleanedPath
                .replaceAll("/", "\\/")
                .replaceAll("\\{\\w+:(.*)}", "$1")
                .replaceAll("\\{\\w+}", "[\\\\w\\\\-\\\\.]+")
                .replaceAll("\\*", ".+");

        final String extractRegex = cleanedPath
                .replaceAll("/", "\\/")
                .replaceAll("\\{(\\w+):(.*)}", "(?<$1>$2)")
                .replaceAll("\\{(\\w+)}", "(?<$1>[\\\\w\\\\-\\\\.]+)")
                .replaceAll("\\*", ".+");
        
        this.pattern = Pattern.compile(String.format("\\/?%s\\/?", regex), Pattern.CASE_INSENSITIVE);
        this.extractPattern = Pattern.compile(String.format("\\/?%s\\/?", extractRegex), Pattern.CASE_INSENSITIVE);

        if(namedGroupsMethod != null) {
            try {
                final Map<String, Integer> namedGroups = (Map<String, Integer>) namedGroupsMethod.invoke(this.extractPattern);
                if(namedGroups != null && !namedGroups.isEmpty()) {
                    variables.putAll(namedGroups);
                }
            } catch (Exception ex){
                logger.error(String.format("Svc can not extract variables from web services path [%s]: %s", path, ex.getMessage()), ex);
            }
        }

        this.key = name.replaceAll("\\{[\\w\\-.]+:([\\w\\-\\.]+)}", "aaaaaaa-filter-zz$1")
                .replaceAll("\\{[\\w\\-.]+}", "aaaaaaa-filter-aa")
                .replaceAll("\\*", "aaaaaaa-filter")
                .toLowerCase();
    }

    /**
     * Gets the name of the web service
     *
     * @return name of the web service
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the HTTP method of the web service
     *
     * @return HTTP method of the web service
     */
    public RestMethod getRestMethod() {
        return restMethod;
    }

    /**
     * Gets the path of the web service
     *
     * @return path of the web service
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the name of the java method
     *
     * @return name of the java method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Gets the type of the parameter declared on java method
     *
     * @return type of the parameter declared on java method
     */
    public MethodParameterType getParameterType() {
        return parameterType;
    }

    /**
     * Gets the type of the response of the java method
     *
     * @return type of the response of the java method
     */
    public WebServiceResponseType getResponseType() {
        return responseType;
    }

    /**
     * Gets the type of the accessor of the java method
     *
     * @return type of the accessor of the java method
     */
    public MethodAccessorType getAccessorType() {
        return accessorType;
    }

    /**
     * Gets the key used to sort the Web Services routes
     *
     * @return key used to sort the Web Services routes
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the class that implements the method
     *
     * @return class that implements the method
     */
    public Class<?> getSvcClass() {
        return svcClass;
    }

    /**
     * Returns the path variables for the given route
     *
     * @param route route to analyze
     * @return the path variables for the given route
     */
    public Json getPathVariables(String route){
        final Json vars = Json.map();
        if(!variables.isEmpty()) {
            route = checkRoute(route);
            if (route != null) {
                final Matcher matcher = extractPattern.matcher(route);
                if (matcher.matches()) {
                    variables.forEach((key, index) -> vars.set(key, matcher.group(index)));
                }
            }
        }
        return vars;
    }

    /**
     * Returns true if the route is valid for the defined path
     *
     * @param route route to check
     * @return true if the route is valid for the defined path
     */
    public boolean isValidRoute(String route){
        return checkRoute(route) != null;
    }

    /**
     * Returns the route if this is valid for the defined path
     *
     * @param route route to check
     * @return the route if this is valid for the defined path
     */
    private String checkRoute(String route){
        if(StringUtils.isBlank(route)){
            route = "";
        } else {
            route = route.trim();
        }
        return pattern.matcher(route).matches() ? route : null;
    }

    @Override
    public int compareTo(RegisteredWebService o) {
        return -1 * getKey().compareTo(o.getKey());
    }
}
