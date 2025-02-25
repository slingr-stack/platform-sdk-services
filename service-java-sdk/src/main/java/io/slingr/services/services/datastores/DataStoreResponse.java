package io.slingr.services.services.datastores;

import io.slingr.services.utils.Json;

import java.util.ArrayList;
import java.util.List;

/**
 * Response to a find request on a data store
 * <p>
 */
public record DataStoreResponse(List<Json> items, int total, String offset) {
    public DataStoreResponse(List<Json> items, int total, String offset) {
        this.items = items != null ? items : new ArrayList<>();
        this.total = total;
        this.offset = offset;
    }

    /**
     * List of documents that fill the query and (if they are used) that are inside of the pagination requirements.
     */
    @Override
    public List<Json> items() {
        return items;
    }

    /**
     * Total of documents that fill the query. It can different to the 'items' size if the pagination is used.
     */
    @Override
    public int total() {
        return total;
    }

    /**
     * Used to paginate results. This must be the value of '_offset' of the next query to do.
     */
    @Override
    public String offset() {
        return offset;
    }
}