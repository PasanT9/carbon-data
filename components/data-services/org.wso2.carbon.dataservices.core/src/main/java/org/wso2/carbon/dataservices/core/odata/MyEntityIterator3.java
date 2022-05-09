package org.wso2.carbon.dataservices.core.odata;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityIterator;

import java.util.Iterator;
import java.util.List;


public class MyEntityIterator3 extends EntityIterator {

    private ODataAdapter adapter;
    private String tableName;
    private String baseURL;

    private List<ODataEntry> entries;

    public static Iterator<Entity> iterator;

    private List<Entity> entityList;

    private ODataEntry properties;

    public MyEntityIterator3(ODataAdapter adapter, String tableName, String baseURL, Iterator<Entity> iterator, List<Entity> entityList, ODataEntry properties) {
        this.adapter = adapter;
        this.tableName = tableName;
        this.baseURL = baseURL;
        this.iterator = iterator;
        this.entityList = entityList;
        this.properties = properties;
    }

    public ODataEntry getProperties() {
        return properties;
    }

    public void setProperties(ODataEntry properties) {
        this.properties = properties;
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

    public List<Entity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<Entity> entityList) {
        this.entityList = entityList;
    }
}