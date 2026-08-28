package com.example.articledelay.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.delay")
public class DelayPublishProperties {

    private String topic = "article.publish";
    private int batchSize = 100;
    private Duration lease = Duration.ofSeconds(30);
    private Duration dispatchInterval = Duration.ofMillis(500);
    private Duration retryBackoff = Duration.ofSeconds(2);
    private Duration recoveryInterval = Duration.ofSeconds(5);
    private Duration compensationInterval = Duration.ofSeconds(30);
    private Duration compensationLookAhead = Duration.ofMinutes(10);
    private int compensationBatchSize = 1000;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Duration getLease() {
        return lease;
    }

    public void setLease(Duration lease) {
        this.lease = lease;
    }

    public Duration getDispatchInterval() {
        return dispatchInterval;
    }

    public void setDispatchInterval(Duration dispatchInterval) {
        this.dispatchInterval = dispatchInterval;
    }

    public Duration getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(Duration retryBackoff) {
        this.retryBackoff = retryBackoff;
    }

    public Duration getRecoveryInterval() {
        return recoveryInterval;
    }

    public void setRecoveryInterval(Duration recoveryInterval) {
        this.recoveryInterval = recoveryInterval;
    }

    public Duration getCompensationInterval() {
        return compensationInterval;
    }

    public void setCompensationInterval(Duration compensationInterval) {
        this.compensationInterval = compensationInterval;
    }

    public Duration getCompensationLookAhead() {
        return compensationLookAhead;
    }

    public void setCompensationLookAhead(Duration compensationLookAhead) {
        this.compensationLookAhead = compensationLookAhead;
    }

    public int getCompensationBatchSize() {
        return compensationBatchSize;
    }

    public void setCompensationBatchSize(int compensationBatchSize) {
        this.compensationBatchSize = compensationBatchSize;
    }
}
