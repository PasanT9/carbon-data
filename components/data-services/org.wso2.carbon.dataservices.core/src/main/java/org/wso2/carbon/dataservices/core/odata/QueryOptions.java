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

    /**
     * Keep count of the skipped entities
     */
    private int skipCount;

    /**
     * Keep count of the processed entities
     */
    private int topCount;

    /**
     * Keep count of the skipped entities for pagination
     */
    private int skipTokenCount;

    /**
     * Number of entities to skip during pagination
     */
    private int itemsToSkip;

    /**
     * Max size of a single page
     */
    private int pageSize;

    /**
     * Link to the next page
     */
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

    /**
     * Initialize all counts to 0.
     */
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

    /**
     * Increment skipCount by 1
     */
    public void stepSkipCount() {
        this.setSkipCount(this.getSkipCount() + 1);
    }

    /**
     * Increment topCount by 1
     */
    public void stepTopCount() {
        this.setTopCount(this.getTopCount() + 1);
    }

    /**
     * Increment skipTokenCount by 1
     */
    public void stepSkipTokenCount() {
        this.setSkipTokenCount(this.getSkipTokenCount() + 1);
    }

    /**
     * Initialize pagination
     * <p>
     * Set page size
     * Set current page
     * Set link to the next page
     */
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
