package com.example.budongbatch.domain.realdeal.client.publicdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Year;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AptItem(
        @JsonProperty("aptNm") String aptNm,
        @JsonProperty("mhouseNm") String mhouseNm,
        @JsonProperty("offiNm") String offiNm,
        @JsonProperty("dealAmount") String dealAmount,
        @JsonProperty("excluUseAr") String excluUseAr,
        @JsonProperty("buildYear") Year buildYear,
        @JsonProperty("umdNm") String umdNm,
        @JsonProperty("jibun") String jibun,
        @JsonProperty("floor") Integer floor
) {
    // 이름 필드 통합 (아파트/빌라/오피스텔)
    public String getName() {
        if (aptNm != null) return aptNm;
        if (mhouseNm != null) return mhouseNm;
        if (offiNm != null) return offiNm;
        return null;
    }
}
