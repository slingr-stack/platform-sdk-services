package io.slingr.services.framework.annotations;

import io.slingr.services.framework.annotations.classes.ClassProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ClassProperty configured on the application and used inside the service. The property must be public or include a
 * public setter-like parameter.
 *
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface ServiceProperty {
    /**
     * Name of the property to use to found the value in the configuration
     */
    String name() default "";

    /**
     * Default value to use on the property if the value is not found on the configuration
     */
    String defaultValue() default ClassProperty.NULL_DEFAULT_VALUE;
}