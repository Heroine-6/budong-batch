package com.example.budongbatch.domain.realdeal.enums;

/**
 * 수집 상태
 *
 * 상태 전이:
 * (신규) → RUNNING  : 수집 시작
 * RUNNING → SUCCESS : 정상 완료
 * RUNNING → FAILED  : 일부 법정동 실패
 * FAILED → RUNNING  : 재시도 시 (실패 법정동만 재수집)
 *
 * 주의:
 * - SUCCESS 상태면 재수집 스킵 (--force로 강제 가능 - 미구현)
 * - RUNNING 상태로 24시간 이상 방치 시 수동 확인 필요
 */
public enum CollectStatus {
    RUNNING,
    SUCCESS,
    FAILED
}
