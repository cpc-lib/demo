package com.example.articledelay.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "article")
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ArticleStatus status;

    @Column(name = "publish_time")
    private Instant publishTime;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "schedule_version", nullable = false)
    private long scheduleVersion;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Article() {
    }

    private Article(String title, String content) {
        this.title = requireText(title, "title");
        this.content = requireText(content, "content");
        this.status = ArticleStatus.DRAFT;
        this.scheduleVersion = 0L;
    }

    public static Article draft(String title, String content) {
        return new Article(title, content);
    }

    public void schedule(Instant publishTime) {
        Objects.requireNonNull(publishTime, "publishTime");
        if (status == ArticleStatus.PUBLISHED) {
            throw new IllegalStateException("Published article cannot be scheduled again");
        }
        this.publishTime = publishTime;
        this.publishedAt = null;
        this.status = ArticleStatus.SCHEDULED;
        this.scheduleVersion++;
    }

    public void cancelSchedule() {
        if (status != ArticleStatus.SCHEDULED) {
            return;
        }
        this.status = ArticleStatus.DRAFT;
        this.publishTime = null;
        this.scheduleVersion++;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public ArticleStatus getStatus() {
        return status;
    }

    public Instant getPublishTime() {
        return publishTime;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public long getScheduleVersion() {
        return scheduleVersion;
    }

    public long getRowVersion() {
        return rowVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
