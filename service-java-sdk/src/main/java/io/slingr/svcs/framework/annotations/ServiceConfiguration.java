package io.slingr.svcs.framework.annotations;

import io.slingr.svcs.utils.Json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ClassProperty that will store the complete service configuration as {@link Json}. The property
 * must be public or include a public setter-like parameter.
 *
 * <p>Created by egonzalez on 25/10/16.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface ServiceConfiguration {
}
