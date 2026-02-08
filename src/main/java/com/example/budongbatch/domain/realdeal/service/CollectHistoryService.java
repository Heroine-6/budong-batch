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

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectHistoryService {

    private final BatchDealCollectHistoryRepository historyRepository;
    private final BatchDealCollectFailedLawdRepository failedLawdRepository;
    private final BatchProperties batchProperties;

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

    public record CollectInitResult(boolean shouldRun, boolean resumed) {
        public static CollectInitResult skip() {
            return new CollectInitResult(false, false);
        }

        public static CollectInitResult start() {
            return new CollectInitResult(true, false);
        }

        public static CollectInitResult resume() {
            return new CollectInitResult(true, true);
        }
    }
}
