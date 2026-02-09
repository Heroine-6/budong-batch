package com.example.budongbatch.domain.realdeal.service;

import com.example.budongbatch.common.config.BatchProperties;
import com.example.budongbatch.domain.realdeal.entity.BatchDealCollectFailedLawd;
import com.example.budongbatch.domain.realdeal.entity.BatchDealCollectHistory;
import com.example.budongbatch.domain.realdeal.enums.CollectStatus;
import com.example.budongbatch.domain.realdeal.repository.BatchDealCollectFailedLawdRepository;
import com.example.budongbatch.domain.realdeal.repository.BatchDealCollectHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 수집 이력 관리 서비스
 *
 * 역할:
 * - 월별 수집 상태 추적 (RUNNING/SUCCESS/FAILED)
 * - 실패한 법정동 기록 및 재시도 지원
 * - RUNNING 상태 타임아웃 처리
 *
 * 사용 흐름:
 * 1. init() - 수집 시작 전 호출, 스킵/시작/재시도 결정
 * 2. resolveTargetLawdCodes() - 수집 대상 법정동 결정 (전체 or 실패분만)
 * 3. finish() - 수집 완료 후 호출, 결과 기록
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectHistoryService {

    private final BatchDealCollectHistoryRepository historyRepository;
    private final BatchDealCollectFailedLawdRepository failedLawdRepository;
    private final BatchProperties batchProperties;

    /**
     * 수집 초기화 - 실행 여부 결정
     *
     * @param dealYmd 수집 대상 월 (예: 202512)
     * @return CollectInitResult
     * - skip: SUCCESS 상태 → 수집 스킵
     * - start: 신규 수집 시작
     * - resume: FAILED/RUNNING 상태 → 재시도
     */
    @Transactional
    public CollectInitResult init(String dealYmd) {
        LocalDateTime now = LocalDateTime.now();
        Optional<BatchDealCollectHistory> existing = historyRepository.findById(dealYmd);

        if (existing.isPresent()) {
            BatchDealCollectHistory history = existing.get();
            Duration timeout = Duration.ofHours(batchProperties.getCollect().getRunningTimeoutHours());
            if (history.getStatus() == CollectStatus.RUNNING
                    && history.getStartedAt() != null
                    && history.getStartedAt().isBefore(now.minus(timeout))) {
                history.markFailedTimeout(now);
                historyRepository.save(history);
                log.warn("RUNNING 상태 타임아웃 처리 - dealYmd={}, startedAt={}",
                        dealYmd, history.getStartedAt());
            }
            if (history.getStatus() == CollectStatus.SUCCESS) {
                return CollectInitResult.skip();
            }
            history.markRunning(now);
            historyRepository.save(history);
            return CollectInitResult.resume();
        }

        historyRepository.save(BatchDealCollectHistory.start(dealYmd, now));
        return CollectInitResult.start();
    }

    /**
     * 수집 대상 법정동 결정
     *
     * - 이전 실패 기록 있음 -> 실패한 법정동만 반환 (부분 재시도)
     * - 실패 기록 없음 -> 전체 법정동 반환 (전체 수집)
     *
     * @param dealYmd 수집 대상 월
     * @param allLawdCodes 전체 법정동 코드 목록
     * @return 수집할 법정동 코드 목록
     */
    @Transactional
    public List<String> resolveTargetLawdCodes(String dealYmd, List<String> allLawdCodes) {
        List<BatchDealCollectFailedLawd> failed = failedLawdRepository.findByDealYmd(dealYmd);
        if (!failed.isEmpty()) {
            List<String> targets = failed.stream()
                    .map(BatchDealCollectFailedLawd::getLawdCd)
                    .collect(Collectors.toList());
            failedLawdRepository.deleteByDealYmd(dealYmd);
            log.info("부분 실패 복구 모드 - 대상 법정동: {}건", targets.size());
            return targets;
        }
        return allLawdCodes;
    }

    /**
     * 수집 완료 처리
     *
     * - 실패 법정동 없음 -> SUCCESS
     * - 실패 법정동 있음 -> FAILED + 실패 목록 저장 (다음 실행 시 재시도 대상)
     *
     * @param dealYmd 수집 대상 월
     * @param collectedCount API에서 수집한 총 건수
     * @param failedLawdCodes 수집 실패한 법정동 코드 목록
     */
    @Transactional
    public void finish(String dealYmd, int collectedCount, List<String> failedLawdCodes) {
        CollectStatus status = failedLawdCodes.isEmpty() ? CollectStatus.SUCCESS : CollectStatus.FAILED;
        BatchDealCollectHistory history = historyRepository.findById(dealYmd)
                .orElseGet(() -> BatchDealCollectHistory.start(dealYmd, LocalDateTime.now()));

        history.finish(status, collectedCount, failedLawdCodes.size(), LocalDateTime.now());
        historyRepository.save(history);

        if (!failedLawdCodes.isEmpty()) {
            List<BatchDealCollectFailedLawd> failures = failedLawdCodes.stream()
                    .map(code -> BatchDealCollectFailedLawd.of(dealYmd, code))
                    .collect(Collectors.toList());
            failedLawdRepository.saveAll(failures);
        }
    }

    /**
     * 수집 초기화 결과
     *
     * @param shouldRun 수집 실행 여부
     * @param resumed 재시도 여부 (true: 실패분 재시도, false: 신규 수집)
     */
    public record CollectInitResult(boolean shouldRun, boolean resumed) {
        /** SUCCESS 상태 - 수집 스킵 */
        public static CollectInitResult skip() {
            return new CollectInitResult(false, false);
        }

        /** 신규 수집 시작 */
        public static CollectInitResult start() {
            return new CollectInitResult(true, false);
        }

        /** FAILED/RUNNING 상태 - 재시도 */
        public static CollectInitResult resume() {
            return new CollectInitResult(true, true);
        }
    }
}
