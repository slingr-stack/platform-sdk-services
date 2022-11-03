package io.slingr.svcs.framework.annotations;

import io.slingr.svcs.Svc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Slingr service. The class must be extend from {@link Svc}
 *
 * <p>Created by lefunes on 25/10/16.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface SlingrService {
    /**
     * Name of the service in camel case format
     */
    String name();
    /**
     * Default prefix to use on auto generated functions
     */
    String functionPrefix() default "";
}
