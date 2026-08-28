package com.example.versioncache.web.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;

/**
 * 更新时必须带上当前 dataVersion，用来触发 MyBatis-Plus 的乐观锁。
 */
public class ArticleUpdateRequest {

    @NotNull
    private Long id;

    @NotBlank
    private String title;

    private String content;

    /**
     * 当前数据版本（从查询结果中带回）
     */
    @NotNull
    private Long dataVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getDataVersion() {
        return dataVersion;
    }

    public void setDataVersion(Long dataVersion) {
        this.dataVersion = dataVersion;
    }
}
