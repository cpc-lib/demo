package com.example.orderjob.job;

public class CloseJobParam {

    private int batchSize = 200;
    private int maxPages = 50;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public void validate() {
        if (batchSize < 1 || batchSize > 2000) {
            throw new IllegalArgumentException("batchSize must be between 1 and 2000");
        }
        if (maxPages < 1 || maxPages > 1000) {
            throw new IllegalArgumentException("maxPages must be between 1 and 1000");
        }
    }
}
