package org.wso2.carbon.dataservices.core.odata;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityIterator;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.server.api.uri.queryoption.*;

import java.net.URI;
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

    private FilterOption filterOption;

    private CountOption countOption;

    private SkipOption skipOption;

    private TopOption topOption;

    private OrderByOption orderByOption;

    private SkipTokenOption skipTokenOption;

    public int currentCount;

    public int rowCount;

    public int skipCount;

    public int topCount;

    public int skipTokenCount;

    private int itemsToSkip;

    private int pageSize;

    public MyEntityIterator2(ODataAdapter adapter, EdmEntitySet edmEntitySet, String baseURL, Iterator<Entity> iterator, List<Entity> entityList, ExpandOption expandOption, FilterOption filterOption, CountOption countOption, SkipOption skipOption,TopOption topOption, OrderByOption orderByOption, SkipTokenOption skipTokenOption,int rowCount, int itemsToSkip, int pageSize, URI nextLinkUri) {
        this.adapter = adapter;
        this.edmEntitySet = edmEntitySet;
        this.baseURL = baseURL;
        this.iterator = iterator;
        this.entityList = entityList;
        this.expandOption = expandOption;
        this.filterOption = filterOption;
        this.countOption = countOption;
        this.skipOption = skipOption;
        this.topOption = topOption;
        this.rowCount = rowCount;
        this.orderByOption = orderByOption;
        this.skipTokenOption = skipTokenOption;
        this.skipCount = 0;
        this.currentCount = 0;
        this.topCount = 0;
        this.skipTokenCount = 0;
        this.itemsToSkip = itemsToSkip;
        this.pageSize = pageSize;

        if(nextLinkUri != null) {
            this.setNext(nextLinkUri);
        }
    }

    public int getItemsToSkip() {
        return itemsToSkip;
    }

    public void setItemsToSkip(int itemsToSkip) {
        this.itemsToSkip = itemsToSkip;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public SkipTokenOption getSkipTokenOption() {
        return skipTokenOption;
    }

    public void setSkipTokenOption(SkipTokenOption skipTokenOption) {
        this.skipTokenOption = skipTokenOption;
    }

    public OrderByOption getOrderByOption() {
        return orderByOption;
    }

    public void setOrderByOption(OrderByOption orderByOption) {
        this.orderByOption = orderByOption;
    }

    public TopOption getTopOption() {
        return topOption;
    }

    public void setTopOption(TopOption topOption) {
        this.topOption = topOption;
    }

    public SkipOption getSkipOption() {
        return skipOption;
    }

    public void setSkipOption(SkipOption skipOption) {
        this.skipOption = skipOption;
    }

    public FilterOption getFilterOption() {
        return filterOption;
    }

    public void setFilterOption(FilterOption filterOption) {
        this.filterOption = filterOption;
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