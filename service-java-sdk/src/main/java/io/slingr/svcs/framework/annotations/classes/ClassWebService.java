package io.slingr.svcs.framework.annotations.classes;

import io.slingr.svcs.framework.annotations.ServiceWebService;
import io.slingr.svcs.framework.annotations.processor.AnnotationsUtils;
import io.slingr.svcs.services.rest.RestMethod;
import io.slingr.svcs.utils.Json;
import io.slingr.svcs.ws.exchange.WebServiceRequest;
import io.slingr.svcs.ws.exchange.WebServiceResponse;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class that represent a web service (a method annotated by {@link ServiceWebService}) found inside a class
 * annotated by <code>@SlingrService</code>
 *
 * Created by lefunes on 10/11/16.
 */
public class ClassWebService {
    public final static String SW_NAME = ServiceWebService.class.getSimpleName();

    private ExecutableElement annotatedElement;
    private final String simpleName;

    private final String path;
    private final List<RestMethod> methods;
    private final String name;

    private final MethodParameterType parameterType;
    private final WebServiceResponseType responseType;
    private final MethodAccessorType accessorType;

    private final Class<?> methodClass;
    private final boolean generated;

    /**
     * Register a service web service in order to generate the {@code Runner} class code
     *
     * @param sWebServiceElement element to process
     * @param generated true if the function is generated automatically and only must be validated if is generated on {@code Runner} on appService.json file
     * @throws IllegalArgumentException if some given parameters is invalid
     */
    public ClassWebService(Element sWebServiceElement, boolean generated) throws IllegalArgumentException {
        this(sWebServiceElement, sWebServiceElement.getAnnotation(ServiceWebService.class), generated);
    }

    /**
     * Register a service web service in order to generate the {@code Runner} class code
     *
     * @param sWebServiceElement element to process
     * @param generated true if the function is generated automatically and only must be validated if is generated on {@code Runner} on appService.json file
     * @throws IllegalArgumentException if some given parameters is invalid
     */
    public ClassWebService(Element sWebServiceElement, ServiceWebService annotation, boolean generated) throws IllegalArgumentException {
        this(sWebServiceElement, annotation != null ? annotation.path() : null, annotation != null ? annotation.methods() : null, null, generated);
    }

    /**
     * Register a service web service in order to generate the {@code Runner} class code
     *
     * @param sWebServiceElement element to process
     * @param processorPath path of the web service
     * @param processorMethods methods of the web service
     * @param methodClass class that implements the method
     * @param generated true if the function is generated automatically and only must be validated if is generated on {@code Runner} on appService.json file
     * @throws IllegalArgumentException if some given parameters is invalid
     */
    public ClassWebService(Element sWebServiceElement, String processorPath, RestMethod[] processorMethods, Class<?> methodClass, boolean generated) throws IllegalArgumentException {
        this.annotatedElement = (ExecutableElement) sWebServiceElement;
        this.simpleName = this.annotatedElement.getSimpleName().toString().trim();

        this.generated = generated;
        this.methodClass = methodClass;

        this.path = normalizePath(processorPath);
        AnnotationsUtils.checkNotEmpty(this.path, "path", SW_NAME, "web service", simpleName);

        this.methods = new ArrayList<>();
        if(processorMethods != null){
            for (RestMethod method : processorMethods) {
                if(!this.methods.contains(method)){
                    this.methods.add(method);
                }
            }
        }
        if(this.methods.isEmpty()){
            this.methods.addAll(RestMethod.all());
        }
        this.methods.sort(Comparator.comparing(Enum::name));

        this.name = generateName(this.path, this.methods);

        final Element typeElement = annotatedElement.getEnclosingElement();
        if (!isValidWebService(typeElement)) {
            throw new IllegalStateException(String.format("Web service [%s] is not valid", getName()));
        }

        parameterType = isValidParameter(typeElement);
        if (parameterType == null) {
            throw new IllegalStateException(String.format("Parameters type for web service [%s] is not valid", getName()));
        }

        responseType = isValidResponse();
        if (responseType == null) {
            throw new IllegalStateException(String.format("Return type for web service [%s] is not valid", getName()));
        }

        if (annotatedElement.getModifiers().contains(Modifier.PROTECTED) || annotatedElement.getModifiers().contains(Modifier.PRIVATE)) {
            accessorType = MethodAccessorType.PRIVATE;
        } else {
            accessorType = MethodAccessorType.PUBLIC;
        }
    }

    /**
     * Generates a name using the given parameters
     *
     * @param path path of the web service
     * @param methods methods to access to the web service
     * @return generated name
     */
    public static String generateName(String path, List<RestMethod> methods) {
        final String m;
        if(methods == null || methods.size() == 0 || methods.size() == RestMethod.values().length){
            m = "all";
        } else {
            m = StringUtils.join(methods.stream().map(RestMethod::toInitials).collect(Collectors.toList()), ":");
        }

        return generateName(path, m);
    }

    /**
     * Generates a name using the given parameters
     *
     * @param path path of the web service
     * @param method methods to access to the web service
     * @return generated name
     */
    public static String generateName(String path, String method) {
        if(StringUtils.isBlank(method)){
            method = "all";
        }
        return String.format("/%s~%s", cleanPath(path), method).toLowerCase();
    }

    /**
     * Cleans the path received
     *
     * @param path path to clean
     * @return cleaned path
     */
    public static String cleanPath(String path) {
        String p = StringUtils.isNotBlank(path) ? path.trim() : "";
        if(p.startsWith("/")){
            p = p.substring(1);
        }
        if(p.endsWith("/")){
            p = p.substring(0, p.length()-1);
        }
        return p;
    }

    /**
     * Normalizes the path received
     *
     * @param path path to clean
     * @return normalized path
     */
    public static String normalizePath(String path) {
        String p = StringUtils.isNotBlank(path) ? path.trim() : "/";
        if(!p.startsWith("/")){
            p = "/"+p;
        }
        if(!p.endsWith("/")){
            p = p+"/";
        }
        return p;
    }

    /**
     * Gets the class that implements the method
     *
     * @return class that implements the method
     */
    public Class<?> getMethodClass() {
        return methodClass;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public List<RestMethod> getMethods() {
        return methods;
    }

    public MethodParameterType getParameterType() {
        return parameterType;
    }

    public WebServiceResponseType getResponseType() {
        return responseType;
    }

    public MethodAccessorType getAccessorType() {
        return accessorType;
    }

    /**
     * Check if the property field defined contains a valid format
     */
    private boolean isValidWebService(Element typeElement) {
        if(annotatedElement.getModifiers().contains(Modifier.ABSTRACT)){
            AnnotationsUtils.error(annotatedElement, "Method [%s] on [%s] must be a non-abstract method", simpleName, typeElement.getSimpleName());
            return false;
        }
        return true;
    }

    /**
     * Check if the property field defined contains a valid format
     */
    private MethodParameterType isValidParameter(Element typeElement) {
        final int parametersCount = annotatedElement.getParameters().size();
        if(parametersCount == 0){
            // no parameters
            return MethodParameterType.NONE;
        }
        if(parametersCount > 1){
            AnnotationsUtils.error(annotatedElement, "Method [%s] on [%s] must accept zero or one parameter (Json, String, WebServiceRequest or Object). Parameters count [%s]", simpleName, typeElement.getSimpleName(), parametersCount);
            return null;
        }
        final VariableElement parameter = annotatedElement.getParameters().get(0);
        final TypeKind kind = parameter.asType().getKind();
        if (!kind.equals(TypeKind.DECLARED)) {
            AnnotationsUtils.error(annotatedElement, "Method [%s] on [%s] must accept zero or one parameter (Json, String, WebServiceRequest or Object). Parameter type [%s]", simpleName, typeElement.getSimpleName(), parameter.asType().toString());
            return null;
        }

        final String className = parameter.asType().toString();
        if (className.equals(Json.class.getCanonicalName())) {
            // JSON parameter
            return MethodParameterType.JSON;
        } else if (className.equals(String.class.getCanonicalName())) {
            // JSON parameter
            return MethodParameterType.STRING;
        } else if (className.equals(WebServiceRequest.class.getCanonicalName())) {
            // WEB SERVICES REQUEST parameter
            return MethodParameterType.REQUEST;
        } else if (className.equals(Object.class.getCanonicalName())) {
            // OBJECT parameter
            return MethodParameterType.OBJECT;
        } else {
            AnnotationsUtils.error(annotatedElement, "Method [%s] on [%s] must accept zero or one parameter (Json, String, WebServiceRequest or Object). Parameter type [%s]", simpleName, typeElement.getSimpleName(), parameter.asType().toString());
            return null;
        }
    }

    /**
     * Check if the return type defined contains a valid format
     */
    private WebServiceResponseType isValidResponse() {
        final TypeMirror returnType = annotatedElement.getReturnType();
        final TypeKind kind = returnType.getKind();
        if(kind == TypeKind.VOID){
            // void return
            return WebServiceResponseType.VOID;
        } else {
            if (kind == TypeKind.DECLARED) {
                if (returnType.toString().equals(Json.class.getCanonicalName())) {
                    // JSON parameter
                    return WebServiceResponseType.JSON;
                } else if (returnType.toString().equals(String.class.getCanonicalName())) {
                    // STRING parameter
                    return WebServiceResponseType.STRING;
                } else if (returnType.toString().equals(WebServiceResponse.class.getCanonicalName())) {
                    // RESPONSE parameter
                    return WebServiceResponseType.RESPONSE;
                } else {
                    // OBJECT return
                    return WebServiceResponseType.OBJECT;
                }
            } else {
                // OBJECT return
                return WebServiceResponseType.OBJECT;
            }
        }
    }
}