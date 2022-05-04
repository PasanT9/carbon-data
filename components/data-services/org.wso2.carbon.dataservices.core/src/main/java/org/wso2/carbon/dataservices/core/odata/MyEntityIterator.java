package org.wso2.carbon.dataservices.core.odata;

import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.EntityIterator;
import org.apache.olingo.commons.api.data.Entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class MyEntityIterator extends EntityIterator {

    private ODataAdapter adapter;
    private String tableName;
    private String baseURL;

    private List<ODataEntry> entries;

    public static Iterator<Entity> iterator;

    public MyEntityIterator(ODataAdapter adapter, String tableName, String baseURL, Iterator<Entity> iterator) {
        this.adapter = adapter;
        this.tableName = tableName;
        this.baseURL = baseURL;
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return this.hasNext();
    }

    @Override
    public Entity next() {
        return this.next();
    }

    public void add(Entity entity) {

    }

    public ODataAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(ODataAdapter adapter) {
        this.adapter = adapter;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public List<ODataEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<ODataEntry> entries) {
        this.entries = entries;
    }

}