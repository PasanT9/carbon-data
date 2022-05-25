package org.wso2.carbon.dataservices.core.odata;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityIterator;
import org.apache.olingo.commons.api.edm.EdmEntitySet;

import java.util.Iterator;
import java.util.List;


public class StreamingEntityIterator extends EntityIterator {

    private ODataAdapter adapter;

    /**
     * Container of the Entity Type
     */
    private EdmEntitySet edmEntitySet;

    /**
     * Base URL of the request
     */
    private String baseURL;

    public static Iterator<Entity> iterator;

    /**
     * List of entities
     */
    private List<Entity> entityList;

    /**
     * OData navigation properties
     */
    private ODataEntry properties;

    /**
     * Name of the table
     */
    private String tableName;

    /**
     * Number of processed entities
     */
    public int entityCount;

    /**
     * Number of entries
     */
    public int rowCount;

    /**
     * OData query options
     */
    private QueryOptions queryOptions;

    public StreamingEntityIterator(ODataAdapter adapter, EdmEntitySet edmEntitySet, String baseURL, Iterator<Entity> iterator, List<Entity> entityList, QueryOptions queryOptions, int rowCount, ODataEntry properties, String tableName) {
        this.adapter = adapter;
        this.edmEntitySet = edmEntitySet;
        this.baseURL = baseURL;
        this.iterator = iterator;
        this.entityList = entityList;
        this.queryOptions = queryOptions;
        this.rowCount = rowCount;
        this.tableName = tableName;
        this.properties = properties;
        this.entityCount = 0;

        if(queryOptions != null && queryOptions.getSkipTokenOption() != null) {
            this.setNext(queryOptions.getNextLinkUri());
        }

        this.setCount(0);
    }

    public QueryOptions getQueryOptions() {
        return queryOptions;
    }

    public ODataEntry getProperties() {
        return properties;
    }

    public void setProperties(ODataEntry properties) {
        this.properties = properties;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public ODataAdapter getAdapter() {
        return adapter;
    }

    public EdmEntitySet getEdmEntitySet() {
        return edmEntitySet;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public List<Entity> getEntityList() {
        return entityList;
    }

    @Override
    public boolean hasNext() {
        return this.hasNext();
    }

    @Override
    public Entity next() {
        return this.next();
    }


}