package io.slingr.services.framework;

import io.slingr.services.framework.annotations.classes.MethodAccessorType;
import io.slingr.services.framework.annotations.classes.MethodParameterType;
import io.slingr.services.framework.annotations.classes.FunctionResponseType;

/**
 * Simple object to keep the information about a service function
 *
 * <p>Created by lefunes on 07/11/16.
 */
public class RegisteredFunction {

    private final String name;
    private final String method;
    private final MethodParameterType parameterType;
    private final FunctionResponseType responseType;
    private final MethodAccessorType accessorType;
    private final Class<?> serviceClass;

    /**
     * Instances a function declared on the service
     *
     * @param name name of the function
     * @param method name of the java method
     * @param parameterType type of the parameter declared on java method
     * @param responseType type of the response of the java method
     * @param accessorType type of the accessor of the java method
     */
    public RegisteredFunction(String name, String method, MethodParameterType parameterType, FunctionResponseType responseType, MethodAccessorType accessorType) {
        this(name, method, parameterType, responseType, accessorType, null);
    }

    /**
     * Instances a function declared on the service
     *
     * @param name name of the function
     * @param method name of the java method
     * @param parameterType type of the parameter declared on java method
     * @param responseType type of the response of the java method
     * @param accessorType type of the accessor of the java method
     * @param serviceClass class that implements the method
     */
    public RegisteredFunction(String name, String method, MethodParameterType parameterType, FunctionResponseType responseType, MethodAccessorType accessorType, Class<?> serviceClass) {
        this.name = name;
        this.method = method;
        this.parameterType = parameterType;
        this.responseType = responseType;
        this.accessorType = accessorType;
        this.serviceClass = serviceClass;
    }

    /**
     * Gets the name of the function
     *
     * @return name of the function
     */
    public String getName() {
        return name;
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
    public FunctionResponseType getResponseType() {
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
     * Gets the class that implements the method
     *
     * @return class that implements the method
     */
    public Class<?> getServiceClass() {
        return serviceClass;
    }
}