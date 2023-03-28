package io.slingr.services.services.datastores;

import io.slingr.services.utils.Json;

import java.util.ArrayList;
import java.util.List;

/**
 * Response to a find request on a data store
 *
 * Created by lefunes on 24/01/17.
 */
public class DataStoreResponse {
    private final List<Json> items;
    private final int total;
    private final String offset;

    public DataStoreResponse(List<Json> items, int total, String offset) {
        this.items = items != null ? items : new ArrayList<>();
        this.total = total;
        this.offset = offset;
    }

    /**
     * List of documents that fill the query and (if they are used) that are inside of the pagination requirements.
     */
    public List<Json> getItems() {
        return items;
    }

    /**
     * Total of documents that fill the query. It can different to the 'items' size if the pagination is used.
     */
    public int getTotal() {
        return total;
    }

    /**
     * Used to paginate results. This must be the value of '_offset' of the next query to do.
     */
    public String getOffset() {
        return offset;
    }
}
