package com.example.budongbatch.common.enums;

/**
 * 지오코딩 상태
 */
public enum GeoStatus {
    PENDING,   // 미처리 (신규)
    SUCCESS,   // 성공
    FAILED,    // 영구 실패 (잘못된 주소, 재시도 X)
    RETRY      // 재시도 대상 (일시적 오류)
}
