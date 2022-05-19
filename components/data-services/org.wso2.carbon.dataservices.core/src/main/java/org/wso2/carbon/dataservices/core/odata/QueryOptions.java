package org.wso2.carbon.dataservices.core.odata;

import org.apache.olingo.server.api.uri.queryoption.*;

import java.net.URI;
import java.net.URISyntaxException;

public class QueryOptions {

    private ExpandOption expandOption;

    private FilterOption filterOption;

    private CountOption countOption;

    private SkipOption skipOption;

    private TopOption topOption;

    private OrderByOption orderByOption;

    private SkipTokenOption skipTokenOption;

    private int skipCount;

    private int topCount;

    private int skipTokenCount;

    private int itemsToSkip;

    private int pageSize;

    private URI nextLinkUri;

    public QueryOptions(ExpandOption expandOption, FilterOption filterOption, CountOption countOption, SkipOption skipOption, TopOption topOption, OrderByOption orderByOption, SkipTokenOption skipTokenOption) {
        this.expandOption = expandOption;
        this.filterOption = filterOption;
        this.countOption = countOption;
        this.skipOption = skipOption;
        this.topOption = topOption;
        this.orderByOption = orderByOption;
        this.skipTokenOption = skipTokenOption;

        initCounts();
    }

    public QueryOptions() {
        this.expandOption = null;
        this.filterOption = null;
        this.countOption = null;
        this.skipOption = null;
        this.topOption = null;
        this.orderByOption = null;
        this.skipTokenOption = null;

        initCounts();
    }

    public ExpandOption getExpandOption() {
        return expandOption;
    }

    public FilterOption getFilterOption() {
        return filterOption;
    }

    public CountOption getCountOption() {
        return countOption;
    }

    public SkipOption getSkipOption() {
        return skipOption;
    }

    public TopOption getTopOption() {
        return topOption;
    }

    public OrderByOption getOrderByOption() {
        return orderByOption;
    }

    public SkipTokenOption getSkipTokenOption() {
        return skipTokenOption;
    }

    private void initCounts() {
        this.skipCount = 0;
        this.skipTokenCount = 0;
        this.topCount = 0;
    }

    public int getSkipCount() {
        return skipCount;
    }

    private void setSkipCount(int skipCount) {
        this.skipCount = skipCount;
    }

    public int getTopCount() {
        return topCount;
    }

    private void setTopCount(int topCount) {
        this.topCount = topCount;
    }

    public int getSkipTokenCount() {
        return skipTokenCount;
    }

    private void setSkipTokenCount(int skipTokenCount) {
        this.skipTokenCount = skipTokenCount;
    }

    public int getItemsToSkip() {
        return itemsToSkip;
    }

    private void setItemsToSkip(int itemsToSkip) {
        this.itemsToSkip = itemsToSkip;
    }

    public int getPageSize() {
        return pageSize;
    }

    private void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void stepSkipCount() {
        this.setSkipCount(this.getSkipCount()+1);
    }

    public void stepTopCount() {
        this.setTopCount(this.getTopCount()+1);
    }

    public void stepSkipTokenCount() {
        this.setSkipTokenCount(this.getSkipTokenCount()+1);
    }

    public void initPagination(int pageSize, String baseURL, String tableName) {
        this.setPageSize(pageSize);

        int page = Integer.parseInt(this.getSkipTokenOption().getValue());
        this.setItemsToSkip(page * pageSize);

        int nextPage = page + 1;
        String nextLink = baseURL + "/" + tableName + "?$skiptoken=" + nextPage;
        try {
            this.setNextLinkUri(nextLink);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public URI getNextLinkUri() {
        return nextLinkUri;
    }

    private void setNextLinkUri(String nextLink) throws URISyntaxException {
        this.nextLinkUri = new URI(nextLink);
    }
}
