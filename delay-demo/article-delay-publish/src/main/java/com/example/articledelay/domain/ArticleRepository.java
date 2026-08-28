package com.example.articledelay.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {


    @Query("""
            select a from Article a
             where (:status is null or a.status = :status)
               and (:keyword is null or lower(a.title) like lower(concat('%', :keyword, '%')))
            """)
    Page<Article> search(
            @Param("keyword") String keyword,
            @Param("status") ArticleStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Article a where a.id = :id")
    Optional<Article> findForUpdateById(@Param("id") Long id);

    List<Article> findByStatusAndPublishTimeLessThanEqualOrderByPublishTimeAsc(
            ArticleStatus status,
            Instant horizon,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Article a
               set a.status = :publishedStatus,
                   a.publishedAt = :publishedAt,
                   a.updatedAt = :publishedAt,
                   a.rowVersion = a.rowVersion + 1
             where a.id = :articleId
               and a.status = :scheduledStatus
               and a.scheduleVersion = :scheduleVersion
               and a.publishTime <= :publishedAt
            """)
    int tryPublish(
            @Param("articleId") Long articleId,
            @Param("scheduleVersion") long scheduleVersion,
            @Param("publishedAt") Instant publishedAt,
            @Param("scheduledStatus") ArticleStatus scheduledStatus,
            @Param("publishedStatus") ArticleStatus publishedStatus
    );
}
