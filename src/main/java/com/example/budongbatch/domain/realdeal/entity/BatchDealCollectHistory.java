package com.example.budongbatch.domain.realdeal.entity;

import com.example.budongbatch.domain.realdeal.enums.CollectStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 월별 수집 이력
 *
 * PK: dealYmd (예: "202512")
 * 용도: 수집 상태 추적, 중복 수집 방지, 재시도 지원
 *
 * @see CollectStatus 상태 전이 규칙
 */
@Entity
@Getter
@Table(name = "batch_deal_collect_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchDealCollectHistory {

    @Id
    @Column(name = "deal_ymd", length = 6, nullable = false)
    private String dealYmd;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CollectStatus status;

    // API에서 수집한 총 건수
    @Column(name = "collected_count")
    private Integer collectedCount;

    @Column(name = "failed_lawd_count")
    private Integer failedLawdCount;

    // 수집 시작 시각
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public static BatchDealCollectHistory start(String dealYmd, LocalDateTime now) {
        BatchDealCollectHistory history = new BatchDealCollectHistory();
        history.dealYmd = dealYmd;
        history.status = CollectStatus.RUNNING;
        history.startedAt = now;
        return history;
    }

    public void markRunning(LocalDateTime now) {
        this.status = CollectStatus.RUNNING;
        this.startedAt = now;
        this.endedAt = null;
    }

    public void finish(CollectStatus status, int collectedCount, int failedLawdCount, LocalDateTime now) {
        this.status = status;
        this.collectedCount = collectedCount;
        this.failedLawdCount = failedLawdCount;
        this.endedAt = now;
    }

    public void markFailedTimeout(LocalDateTime now) {
        this.status = CollectStatus.FAILED;
        this.endedAt = now;
    }
}
