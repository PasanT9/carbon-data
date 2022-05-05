package org.wso2.carbon.dataservices.core.odata;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityIterator;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;

import java.util.Iterator;
import java.util.List;


public class MyEntityIterator2 extends EntityIterator {

    private ODataAdapter adapter;
    private EdmEntitySet edmEntitySet;
    private String baseURL;

    private List<ODataEntry> entries;

    public static Iterator<Entity> iterator;

    private List<Entity> entityList;

    private ExpandOption expandOption;

    public MyEntityIterator2(ODataAdapter adapter, EdmEntitySet edmEntitySet, String baseURL, Iterator<Entity> iterator, List<Entity> entityList, ExpandOption expandOption) {
        this.adapter = adapter;
        this.edmEntitySet = edmEntitySet;
        this.baseURL = baseURL;
        this.iterator = iterator;
        this.entityList = entityList;
        this.expandOption = expandOption;
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

    public EdmEntitySet getEdmEntitySet() {
        return edmEntitySet;
    }

    public void setEdmEntitySet(EdmEntitySet edmEntitySet) {
        this.edmEntitySet = edmEntitySet;
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

    public ExpandOption getExpandOption() {
        return expandOption;
    }

    public void setExpandOption(ExpandOption expandOption) {
        this.expandOption = expandOption;
    }
}