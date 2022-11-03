package io.slingr.svcs.framework.annotations;

import java.lang.annotation.*;

/**
 * Annotation that permits to include more than one web service per method
 *
 * <p>Created by lefunes on 16/11/16.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface ServiceWebServices {
    ServiceWebService[] value();
}