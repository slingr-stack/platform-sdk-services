package io.slingr.svcs.framework.annotations;

import java.lang.annotation.*;

/**
 * Function provided by the service and used by the application.
 *
 * <p>Created by lefunes on 03/11/16.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Repeatable(ServiceFunctions.class)
public @interface ServiceFunction {
    /**
     * Name of the function to use
     */
    String name() default "";
}
