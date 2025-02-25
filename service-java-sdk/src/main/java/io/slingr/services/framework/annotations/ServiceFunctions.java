package io.slingr.services.framework.annotations;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that permits to include more than one function per method.
 *
 * <p>Created by lefunes on 16/11/16.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface ServiceFunctions {
    ServiceFunction[] value();
}