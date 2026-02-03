package com.example.budongbatch.fixture;

import com.example.budongbatch.domain.realdeal.client.publicdata.AptItem;

import java.time.Year;

/**
 * AptItem 테스트 Fixture
 * 기본 데이터를 제공하고, 특정 필드만 변경된 인스턴스 생성
 */
public class AptItemFixture {

    // 기본값
    private static final String DEFAULT_APT_NM = "래미안아파트";
    private static final String DEFAULT_DEAL_AMOUNT = "12,500";
    private static final String DEFAULT_EXCLU_USE_AR = "84.99";
    private static final Year DEFAULT_BUILD_YEAR = Year.of(2020);
    private static final Integer DEFAULT_DEAL_YEAR = 2025;
    private static final Integer DEFAULT_DEAL_MONTH = 1;
    private static final Integer DEFAULT_DEAL_DAY = 15;
    private static final String DEFAULT_UMD_NM = "역삼동";
    private static final String DEFAULT_JIBUN = "123-45";
    private static final Integer DEFAULT_FLOOR = 10;

    /**
     * 기본 아파트 데이터
     */
    public static AptItem create() {
        return new AptItem(
                DEFAULT_APT_NM, null, null,
                DEFAULT_DEAL_AMOUNT, DEFAULT_EXCLU_USE_AR, DEFAULT_BUILD_YEAR,
                DEFAULT_DEAL_YEAR, DEFAULT_DEAL_MONTH, DEFAULT_DEAL_DAY,
                DEFAULT_UMD_NM, DEFAULT_JIBUN, DEFAULT_FLOOR
        );
    }

    /**
     * 이름이 null인 데이터
     */
    public static AptItem withNullName() {
        return new AptItem(
                null, null, null,
                DEFAULT_DEAL_AMOUNT, DEFAULT_EXCLU_USE_AR, DEFAULT_BUILD_YEAR,
                DEFAULT_DEAL_YEAR, DEFAULT_DEAL_MONTH, DEFAULT_DEAL_DAY,
                DEFAULT_UMD_NM, DEFAULT_JIBUN, DEFAULT_FLOOR
        );
    }

    /**
     * 거래일자가 null인 데이터
     */
    public static AptItem withNullDealDate() {
        return new AptItem(
                DEFAULT_APT_NM, null, null,
                DEFAULT_DEAL_AMOUNT, DEFAULT_EXCLU_USE_AR, DEFAULT_BUILD_YEAR,
                null, null, null,
                DEFAULT_UMD_NM, DEFAULT_JIBUN, DEFAULT_FLOOR
        );
    }

    /**
     * 거래금액이 유효하지 않은 데이터
     */
    public static AptItem withInvalidDealAmount() {
        return new AptItem(
                DEFAULT_APT_NM, null, null,
                "invalid", DEFAULT_EXCLU_USE_AR, DEFAULT_BUILD_YEAR,
                DEFAULT_DEAL_YEAR, DEFAULT_DEAL_MONTH, DEFAULT_DEAL_DAY,
                DEFAULT_UMD_NM, DEFAULT_JIBUN, DEFAULT_FLOOR
        );
    }

    /**
     * 오피스텔 데이터 (offiNm 사용)
     */
    public static AptItem officetel() {
        return new AptItem(
                null, null, "강남오피스텔",
                "8,000", "33.5", Year.of(2018),
                DEFAULT_DEAL_YEAR, DEFAULT_DEAL_MONTH, 20,
                "논현동", "55-1", 5
        );
    }
}
