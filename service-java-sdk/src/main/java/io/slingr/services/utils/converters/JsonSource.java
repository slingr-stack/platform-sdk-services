package io.slingr.services.utils.converters;

import io.slingr.services.utils.Json;

/**
 * This interface is implemented by the objects that can be converted to a Json object
 *
 */
public interface JsonSource {
    /**
     * Object is converted to a Json object
     *
     * @return Json object
     */
    Json toJson();
}