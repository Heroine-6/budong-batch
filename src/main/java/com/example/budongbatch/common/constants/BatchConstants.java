package com.example.budongbatch.common.constants;

/**
 * 배치 처리 관련 공통 상수
 */
public final class BatchConstants {

    private BatchConstants() {}

    // 지오코딩 관련
    public static final int GEOCODE_MAX_RETRY = 3;
    public static final int GEOCODE_CHUNK_SIZE = 4000;
    public static final int GEOCODE_PAGE_SIZE = 4000;

    // ES 색인 관련
    public static final int INDEX_CHUNK_SIZE = 2000;
    public static final int INDEX_PAGE_SIZE = 2000;
}
