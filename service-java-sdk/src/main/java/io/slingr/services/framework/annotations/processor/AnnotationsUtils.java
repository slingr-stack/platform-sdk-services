package io.slingr.services.framework.annotations.processor;

import io.slingr.services.framework.annotations.classes.PropertyType;
import io.slingr.services.utils.Json;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utils to help to compile the code of the service
 *
 * <p>Created by lefunes on 02/11/16.
 */
public abstract class AnnotationsUtils {

    private static Messager messager = null;
    private static Types typeUtils = null;

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Throws an exception if the util is not initialized yet
     */
    public static void checkIfInitialized(){
        if(!initialized.get()){
            throw new IllegalStateException("Annotation utils not initialed yet");
        }
    }

    /**
     * Sets the basic parameters for the Annotation utils
     *
     * @param environment processing environment
     */
    public static void set(ProcessingEnvironment environment) {
        typeUtils = environment.getTypeUtils();
        messager = environment.getMessager();
        initialized.set(true);
    }

    /**
     * Log the message as a compilation message.
     *
     * @param e checked element
     * @param msg formatted message to show
     * @param args parameters to use in the formatted message
     */
    public static void note(Element e, String msg, Object... args) {
        checkIfInitialized();
        
        messager.printMessage(Diagnostic.Kind.NOTE, String.format(msg, args), e);
    }

    /**
     * Logs the message as a compile error
     *
     * @param e checked element
     * @param msg formatted error message to show
     * @param args parameters to use in the formatted message
     */
    public static void error(Element e, String msg, Object... args) {
        checkIfInitialized();

        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
    }

    /**
     * Returns the element represented by the class type
     */
    public static Element asElement(TypeMirror classType){
        checkIfInitialized();

        return typeUtils.asElement(classType);
    }

    /**
     * Checks that the <code>var</code> string is not empty
     *
     * @param var object to check
     * @param annotationProperty name of the annotation property
     * @param annotation annotation used to annotate the element that contains the property
     * @param typeAnnotated type of object that represent the annotated element
     * @param nameAnnotated name of the annotated object
     */
    public static void checkNotEmpty(String var, String annotationProperty, String annotation, String typeAnnotated, String nameAnnotated) {
        if (StringUtils.isEmpty(var)) {
            throw new IllegalArgumentException(String.format("Empty [%s] in @%s for %s [%s]", annotationProperty, annotation, typeAnnotated, nameAnnotated));
        }
    }

    /**
     * Checks that the <code>var</code> object is not null
     *
     * @param var object to check
     * @param annotationProperty name of the annotation property
     * @param annotation annotation used to annotate the element that contains the property
     * @param typeAnnotated type of object that represent the annotated element
     * @param nameAnnotated name of the annotated object
     */
    public static void checkNotNull(Object var, String annotationProperty, String annotation, String typeAnnotated, String nameAnnotated) {
        if (var == null) {
            throw new IllegalArgumentException(String.format("Empty [%s] in @%s for %s [%s]", annotationProperty, annotation, typeAnnotated, nameAnnotated));
        }
    }

    /**
     * 
     */
    public static PropertyType isValid(Class<?> clazz, VariableElement element, String name, boolean isProperty, String accessor) {
        return isValid(clazz, element, name, isProperty, accessor, false);
    }

    /**
     * 
     */
    public static PropertyType isValid(Class<?> clazz, VariableElement element, String name, boolean isProperty, String accessor, boolean isServiceConfiguration) {
        final PropertyType type;
        if(isProperty) {
            type = isValidProperty(clazz, element, name, accessor, isServiceConfiguration);
        } else {
            type = isValidSetter2(clazz, element, name, accessor, isServiceConfiguration);
        }
        return type;
    }

    /**
     * 
     */
    public static PropertyType isValidProperty(Class<?> clazz, VariableElement element, String name, String property) {
        return isValidProperty(clazz, element, name, property, false);
    }

    /**
     * 
     */
    public static PropertyType isValidProperty(Class<?> clazz, VariableElement element, String name, String property, boolean isServiceConfiguration) {
        PropertyType type = PropertyType.OTHER;
        final Element typeElement = element.getEnclosingElement();

        if(clazz == null){
            type = isValidPropertyVariable(element, isServiceConfiguration);
            if (type == null) {
                error(element, "ServiceProperty [%s] on [%s] must be String, Boolean or Json in order to set [%s]", property, typeElement.getSimpleName(), name);
                return null;
            }
        } else {
            boolean valid = isValidVariable(clazz, element);
            if (!valid) {
                error(element, "ServiceProperty [%s] on [%s] must be of type %s in order to set [%s]", property, typeElement.getSimpleName(), clazz.getSimpleName(), name);
                return null;
            }
        }

        return type;
    }

    /**
     * 
     */
    public static PropertyType isValidSetter(Class<?> clazz, VariableElement element, String name, String setter){
        return isValidSetter(clazz, element, name, setter, false);
    }

    /**
     * 
     */
    public static PropertyType isValidSetter(Class<?> clazz, VariableElement element, String name, String setter, boolean isServiceConfiguration) {
        PropertyType type = PropertyType.OTHER;

        final Element typeElement = element.getEnclosingElement();

        // check if accessor exists
        Element am = null;
        for (Element tElement : typeElement.getEnclosedElements()) {
            if(tElement.getSimpleName().toString().equals(setter)){
                am = tElement;
                break;
            }
        }
        if(am == null){
            note(element, "Accessor [%s()] was not found for %s [%s] on [%s]", setter, clazz == null ? "property" : clazz.getSimpleName(), name, typeElement.getSimpleName());
            return null;
        }

        if(!am.getKind().equals(ElementKind.METHOD)){
            note(element, "Accessor [%s()] must be a public method on [%s] in order to set [%s]", setter, typeElement.getSimpleName(), name);
            return null;
        }

        if(am.getModifiers().contains(Modifier.ABSTRACT)){
            note(element, "Accessor [%s()] on [%s] must be a non-abstract method in order to set [%s]", setter, typeElement.getSimpleName(), name);
            return null;
        }

        if(am.getModifiers().contains(Modifier.PROTECTED) || am.getModifiers().contains(Modifier.PRIVATE)){
            note(element, "Accessor [%s()] must be public on [%s] in order to set [%s]", setter, typeElement.getSimpleName(), name);
            return null;
        }

        if(clazz == null){
            final ExecutableElement method = (ExecutableElement) am;
            if(method.getParameters().size() != 1){
                note(element, "Accessor [%s()] on [%s] must accept only one parameter of type either String or Boolean in order to set [%s]", setter, typeElement.getSimpleName(), name);
                return null;
            }
            final VariableElement parameter = method.getParameters().get(0);
            type = isValidPropertyVariable(parameter, isServiceConfiguration);
            if (type == null) {
                note(element, "Accessor [%s()] on [%s] must accept only one parameter of type either String or Boolean in order to set [%s]", setter, typeElement.getSimpleName(), name);
                return null;
            }
        } else {
            final ExecutableElement method = (ExecutableElement) am;
            if (method.getParameters().size() != 1) {
                note(element, "Accessor [%s()] on [%s] must accept only one parameter of type %s in order to set [%s]", setter, typeElement.getSimpleName(), clazz.getSimpleName(), name);
                return null;
            }
            final VariableElement parameter = method.getParameters().get(0);
            boolean valid = isValidVariable(clazz, parameter);
            if (!valid) {
                note(element, "Accessor [%s()] on [%s] must accept only one parameter of type %s in order to set [%s]", setter, typeElement.getSimpleName(), clazz.getSimpleName(), name);
                return null;
            }
        }

        return type;
    }

    /**
     * 
     */
    public static PropertyType isValidSetter2(Class<?> clazz, VariableElement element, String name, String setter) {
        return isValidSetter2(clazz, element, name, setter, false);
    }

    /**
     * 
     */
    public static PropertyType isValidSetter2(Class<?> clazz, VariableElement element, String name, String setter, boolean isServiceConfiguration) {
        PropertyType type = PropertyType.OTHER;

        final Element typeElement = element.getEnclosingElement();

        // check if accessor exists
        Element am = null;
        for (Element tElement : typeElement.getEnclosedElements()) {
            if(tElement.getSimpleName().toString().equals(setter)){
                am = tElement;
                break;
            }
        }
        if(am == null){
            error(element, "Accessor [%s()] was not found for %s [%s] on [%s]", setter, clazz == null ? "property" : clazz.getSimpleName(), name, typeElement.getSimpleName());
            return null;
        }

        if(!am.getKind().equals(ElementKind.METHOD)){
            error(element, "Accessor [%s()] must be a public method on [%s] in order to set [%s]", setter, typeElement.getSimpleName(), name);
            return null;
        }

        if(!am.getModifiers().contains(Modifier.PUBLIC)){
            error(element, "Accessor [%s()] must be public on [%s] in order to set [%s]", setter, typeElement.getSimpleName(), name);
            return null;
        }

        if(am.getModifiers().contains(Modifier.ABSTRACT)){
            error(element, "Accessor [%s()] on [%s] must be a non-abstract method in order to set [%s]", setter, typeElement.getSimpleName(), name);
            return null;
        }

        if(clazz == null){
            final ExecutableElement method = (ExecutableElement) am;
            if(method.getParameters().size() != 1){
                error(element, "Accessor [%s()] on [%s] must accept only one parameter of type either String or Boolean in order to set [%s]", setter, typeElement.getSimpleName(), name);
                return null;
            }
            final VariableElement parameter = method.getParameters().get(0);
            type = isValidPropertyVariable(parameter, isServiceConfiguration);
            if (type == null) {
                error(element, "Accessor [%s()] on [%s] must accept only one parameter of type either String or Boolean in order to set [%s]", setter, typeElement.getSimpleName(), name);
                return null;
            }
        } else {
            final ExecutableElement method = (ExecutableElement) am;
            if (method.getParameters().size() != 1) {
                error(element, "Accessor [%s()] on [%s] must accept only one parameter of type %s in order to set [%s]", setter, typeElement.getSimpleName(), clazz.getSimpleName(), name);
                return null;
            }
            final VariableElement parameter = method.getParameters().get(0);
            boolean valid = isValidVariable(clazz, parameter);
            if (!valid) {
                error(element, "Accessor [%s()] on [%s] must accept only one parameter of type %s in order to set [%s]", setter, typeElement.getSimpleName(), clazz.getSimpleName(), name);
                return null;
            }
        }

        return type;
    }

    /**
     * Checks if the variable that represent the class has a valid format
     */
    public static boolean isValidVariable(Class<?> clazz, VariableElement parameter) {
        return parameter.asType().getKind().equals(TypeKind.DECLARED) &&
                parameter.asType().toString().equals(clazz.getCanonicalName());
    }

    /**
     * Returns the equivalent property type to the given parameter
     *
     * @param parameter parameter to check
     * @return equivalent property or null if parameter is invalid
     */
    private static PropertyType isValidPropertyVariable(VariableElement parameter){
        return isValidPropertyVariable(parameter, false);
    }

    /**
     * Returns the equivalent property type to the given parameter
     *
     * @param parameter parameter to check
     * @param isServiceConfiguration true if the property represent a service configuration
     * @return equivalent property or null if parameter is invalid
     */
    private static PropertyType isValidPropertyVariable(VariableElement parameter, boolean isServiceConfiguration) {
        PropertyType type = null;
        final TypeKind kind = parameter.asType().getKind();
        if(kind.isPrimitive()){
            if (kind.equals(TypeKind.BOOLEAN)) {
                // boolean (primitive)
                type = PropertyType.BOOLEAN;
            }
        } else if (kind.equals(TypeKind.DECLARED)) {
            if (parameter.asType().toString().equals(Boolean.class.getCanonicalName())) {
                // boolean
                type = PropertyType.BOOLEAN;
            } else if (parameter.asType().toString().equals(String.class.getCanonicalName())) {
                // string
                type = PropertyType.STRING;
            } else if (parameter.asType().toString().equals(Json.class.getCanonicalName())) {
                // json
                if(isServiceConfiguration) {
                    type = PropertyType.CONFIG;
                } else {
                    type = PropertyType.JSON;
                }
            }
        }
        return type;
    }
}