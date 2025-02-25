package io.slingr.services.framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Service data store to use inside the service. The property must be public or include a public setter-like
 * parameter.
 *
 * <p>Created by lefunes on 16/11/16.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface ServiceDataStore {
    /**
     * Name of the data store
     */
    String name() default "";
}