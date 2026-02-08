package com.example.budongbatch.domain.realdeal.entity;

import java.io.Serializable;
import java.util.Objects;

public class BatchDealCollectFailedLawdId implements Serializable {
    private String dealYmd;
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
