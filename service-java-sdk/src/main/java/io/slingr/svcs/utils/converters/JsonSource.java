package io.slingr.svcs.utils.converters;

import io.slingr.svcs.utils.Json;

/**
 * This interface is implemented by the objects that can be converted to a Json object
 *
 * <p>Created by lefunes on 15/03/18.
 */
public interface JsonSource {
    /**
     * Object is converted to a Json object
     *
     * @return Json object
     */
    Json toJson();
}
