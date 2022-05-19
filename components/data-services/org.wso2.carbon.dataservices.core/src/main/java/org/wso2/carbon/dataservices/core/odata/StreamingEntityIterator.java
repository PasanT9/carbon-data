package org.wso2.carbon.dataservices.core.odata;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityIterator;
import org.apache.olingo.commons.api.edm.EdmEntitySet;

import java.net.URI;
import java.util.Iterator;
import java.util.List;


public class StreamingEntityIterator extends EntityIterator {

    private ODataAdapter adapter;
    private EdmEntitySet edmEntitySet;
    private String baseURL;

    public static Iterator<Entity> iterator;

    private List<Entity> entityList;

    private ODataEntry properties;

    private String tableName;

    public int EntityCount;

    public int rowCount;

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
        this.EntityCount = 0;

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