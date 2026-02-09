package com.example.budongbatch.domain.realdeal.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * 수집 실패 법정동 복합키
 *
 * PK: (dealYmd, lawdCd)
 * 예: ("202512", "11110") -> 2025년 12월 종로구 수집 실패
 */
public class BatchDealCollectFailedLawdId implements Serializable {
    // 수집 대상 월 (YYYYMM)
    private String dealYmd;
    // 법정동 코드 (5자리)
    private String lawdCd;

    public BatchDealCollectFailedLawdId() {}

    public BatchDealCollectFailedLawdId(String dealYmd, String lawdCd) {
        this.dealYmd = dealYmd;
        this.lawdCd = lawdCd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchDealCollectFailedLawdId that = (BatchDealCollectFailedLawdId) o;
        return Objects.equals(dealYmd, that.dealYmd) && Objects.equals(lawdCd, that.lawdCd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dealYmd, lawdCd);
    }
}
