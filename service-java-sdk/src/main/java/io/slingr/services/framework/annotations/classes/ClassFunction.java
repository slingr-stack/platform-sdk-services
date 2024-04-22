package io.slingr.services.framework.annotations.classes;

import io.slingr.services.framework.annotations.ServiceFunction;
import io.slingr.services.framework.annotations.SlingrService;
import io.slingr.services.framework.annotations.processor.AnnotationsUtils;
import io.slingr.services.utils.Json;
import io.slingr.services.ws.exchange.FunctionRequest;
import org.apache.commons.lang3.StringUtils;

import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Class that represent a function (a method annotated by {@link ServiceFunction})
 * found inside a class annotated by  {@link SlingrService}
 *
 * <p>Created by lefunes on 04/11/16.
 */
public class ClassFunction {
    public final static String SF_NAME = ServiceFunction.class.getSimpleName();

    private final ExecutableElement annotatedElement;
    private final String simpleName;

    private final String name;
    private final MethodParameterType parameterType;
    private final FunctionResponseType responseType;
    private final MethodAccessorType accessorType;
    private final Class<?> methodClass;
    private final boolean generated;

    /**
     * Register a service function in order to generate the {@code Runner} class code
     *
     * @param sFunctionElement element to process
     * @param generated true if the function is generated automatically and only must be validated if is generated on {@code Runner} on appService.json file
     * @throws IllegalArgumentException if some given parameters is invalid
     */
    public ClassFunction(Element sFunctionElement, boolean generated) throws IllegalArgumentException {
        this(sFunctionElement, sFunctionElement.getAnnotation(ServiceFunction.class), generated);
    }

    /**
     * Register a service function in order to generate the {@code Runner} class code
     *
     * @param sFunctionElement element to process
     * @param annotation annotation to process
     * @param generated true if the function is generated automatically and only must be validated if is generated on {@code Runner} on appService.json file
     * @throws IllegalArgumentException if some given parameters is invalid
     */
    public ClassFunction(Element sFunctionElement, ServiceFunction annotation, boolean generated) throws IllegalArgumentException {
        this(sFunctionElement, annotation != null ? annotation.name() : null, generated);
    }

    /**
     * Register a service function in order to generate the {@code Runner} class code
     *
     * @param sFunctionElement element to process
     * @param serviceFunctionName name of the function
     * @param generated true if the function is generated automatically and only must be validated if is generated on {@code Runner} on appService.json file
     * @throws IllegalArgumentException if some given parameters is invalid
     */
    public ClassFunction(Element sFunctionElement, String serviceFunctionName, boolean generated) throws IllegalArgumentException {
        this(sFunctionElement, serviceFunctionName, null, generated);
    }

    /**
     * Register a service function in order to generate the {@code Runner} class code
     *
     * @param sFunctionElement element to process
     * @param serviceFunctionName name of the function
     * @param methodClass class that implements the method
     * @param generated true if the function is generated automatically and only must be validated if is generated on {@code Runner} on appService.json file
     * @throws IllegalArgumentException if some given parameters is invalid
     */
    public ClassFunction(Element sFunctionElement, String serviceFunctionName, Class<?> methodClass, boolean generated) throws IllegalArgumentException {
        this.generated = generated;
        this.annotatedElement = (ExecutableElement) sFunctionElement;
        this.simpleName = this.annotatedElement.getSimpleName().toString().trim();

        name = StringUtils.isNotBlank(serviceFunctionName) ? serviceFunctionName : simpleName;

        this.methodClass = methodClass;

        AnnotationsUtils.checkNotEmpty(name, "name", SF_NAME, "method", name);

        final Element typeElement = annotatedElement.getEnclosingElement();
        if (!isValidFunction(typeElement)) {
            throw new IllegalStateException(String.format("Method [%s] is not valid", getName()));
        }

        parameterType = hasValidParameters(typeElement);
        if (parameterType == null) {
            throw new IllegalStateException(String.format("Parameters for method [%s] is not valid", getName()));
        }

        responseType = getResponse();

        if (annotatedElement.getModifiers().contains(Modifier.PROTECTED) || annotatedElement.getModifiers().contains(Modifier.PRIVATE)) {
            accessorType = MethodAccessorType.PRIVATE;
        } else {
            accessorType = MethodAccessorType.PUBLIC;
        }
    }

    /**
     * Generates a clone of the function with the new name
     *
     * @param serviceFunctionName new name
     * @param generated true if the function is generated automatically and only must be validated if is generated on {@code Runner} on appService.json file
     * @return clone of the function
     * @throws IllegalArgumentException if some given parameters is invalid
     */
    public ClassFunction clone(String serviceFunctionName, boolean generated){
        return new ClassFunction(this.annotatedElement, serviceFunctionName, this.methodClass, generated);
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getName() {
        return name;
    }

    public FunctionResponseType getResponseType() {
        return responseType;
    }

    public MethodParameterType getParameterType() {
        return parameterType;
    }

    public MethodAccessorType getAccessorType() {
        return accessorType;
    }

    /**
     * Gets the class that implements the method
     *
     * @return class that implements the method
     */
    public Class<?> getMethodClass() {
        return methodClass;
    }

    /**
     * Returns true if the function is generated automatically and only must be validated if is generated on {@code Runner} on appService.json file
     *
     * @return true if the function is generated automatically
     */
    public boolean isGenerated() {
        return generated;
    }

    /**
     * Check if the property field defined contains a valid format
     */
    private boolean isValidFunction(Element typeElement) {
        if(annotatedElement.getModifiers().contains(Modifier.ABSTRACT)){
            AnnotationsUtils.error(annotatedElement, "Method [%s] on [%s] must be a non-abstract method", simpleName, typeElement.getSimpleName());
            return false;
        }
        return true;
    }

    /**
     * Check if the property field defined contains a valid format
     */
    private MethodParameterType hasValidParameters(Element typeElement) {
        final int parametersCount = annotatedElement.getParameters().size();
        if(parametersCount == 0){
            // no parameters
            return MethodParameterType.NONE;
        }
        if(parametersCount > 1){
            AnnotationsUtils.error(annotatedElement, "Method [%s] on [%s] must accept zero or one parameter (Object, String, Json or FunctionRequest). Parameters count [%s]", simpleName, typeElement.getSimpleName(), parametersCount);
            return null;
        }
        final VariableElement parameter = annotatedElement.getParameters().get(0);
        final TypeKind kind = parameter.asType().getKind();
        if (!kind.equals(TypeKind.DECLARED)) {
            AnnotationsUtils.error(annotatedElement, "Method [%s] on [%s] must accept zero or one parameter (Object, String, Json or FunctionRequest). Parameter type [%s]", simpleName, typeElement.getSimpleName(), parameter.asType().toString());
            return null;
        }

        if (parameter.asType().toString().equals(Json.class.getCanonicalName())) {
            // JSON parameter
            return MethodParameterType.JSON;
        } else if (parameter.asType().toString().equals(FunctionRequest.class.getCanonicalName())) {
            // FUNCTION REQUEST parameter
            return MethodParameterType.REQUEST;
        } else if (parameter.asType().toString().equals(String.class.getCanonicalName())) {
            // STRING parameter
            return MethodParameterType.STRING;
        } else if (parameter.asType().toString().equals(Object.class.getCanonicalName())) {
            // OBJECT parameter
            return MethodParameterType.OBJECT;
        } else {
            AnnotationsUtils.error(annotatedElement, "Method [%s] on [%s] must accept zero or one parameter (Object, String, Json or FunctionRequest). Parameter type [%s]", simpleName, typeElement.getSimpleName(), parameter.asType().toString());
            return null;
        }
    }

    /**
     * Check if the return type defined contains a valid format
     */
    private FunctionResponseType getResponse() {
        final TypeMirror returnType = annotatedElement.getReturnType();
        final TypeKind kind = returnType.getKind();
        if(kind == TypeKind.VOID){
            // void return
            return FunctionResponseType.VOID;
        } else {
            if (kind == TypeKind.DECLARED) {
                if (returnType.toString().equals(Json.class.getCanonicalName())) {
                    // JSON parameter
                    return FunctionResponseType.JSON;
                }
            }
            return FunctionResponseType.OTHER;
        }
    }
}
