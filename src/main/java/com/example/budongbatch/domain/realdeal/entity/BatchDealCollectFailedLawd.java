package com.example.budongbatch.domain.realdeal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "batch_deal_collect_failed_lawd")
@IdClass(BatchDealCollectFailedLawdId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchDealCollectFailedLawd {

    @Id
    @Column(name = "deal_ymd", length = 6, nullable = false)
    private String dealYmd;

    @Id
    @Column(name = "lawd_cd", length = 10, nullable = false)
    private String lawdCd;

    public static BatchDealCollectFailedLawd of(String dealYmd, String lawdCd) {
        BatchDealCollectFailedLawd failed = new BatchDealCollectFailedLawd();
        failed.dealYmd = dealYmd;
        failed.lawdCd = lawdCd;
        return failed;
    }
}
