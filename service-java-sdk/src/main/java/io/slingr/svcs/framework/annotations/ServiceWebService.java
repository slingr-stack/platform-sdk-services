package io.slingr.svcs.framework.annotations;

import io.slingr.svcs.services.rest.RestMethod;

import java.lang.annotation.*;

/**
 * Web Service provided by the svc and used by the service to send notification to the application. The function must
 * be public.
 *
 * <p>Created by lefunes on 03/11/16.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(ServiceWebServices.class)
public @interface ServiceWebService {
    /**
     * Path of the web service
     */
    String path() default "";

    /**
     * Methods accepted by this web service
     */
    RestMethod[] methods() default {};
}
