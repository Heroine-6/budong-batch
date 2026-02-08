package com.example.budongbatch.domain.realdeal.entity;

import com.example.budongbatch.domain.realdeal.enums.CollectStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(name = "collected_count")
    private Integer collectedCount;

    @Column(name = "failed_lawd_count")
    private Integer failedLawdCount;

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
