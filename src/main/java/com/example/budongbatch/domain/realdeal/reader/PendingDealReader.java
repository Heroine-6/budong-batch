package com.example.budongbatch.domain.realdeal.reader;

import com.example.budongbatch.common.enums.GeoStatus;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.domain.realdeal.repository.RealDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * 지오코딩 대상 Reader
 *
 * PENDING 상태 (신규) 또는 RETRY 상태 (재시도 대상) 조회
 * 페이지 단위로 조회하여 메모리 효율성 확보
 */
@Component
@RequiredArgsConstructor
public class PendingDealReader implements ItemReader<RealDeal> {

    private final RealDealRepository realDealRepository;

    private static final int PAGE_SIZE = 100;
    private static final int MAX_RETRY = 3;

    private Iterator<RealDeal> currentIterator;
    private boolean pendingExhausted = false;

    @Override
    public RealDeal read() {
        // 현재 iterator에 데이터가 있으면 반환
        if (currentIterator != null && currentIterator.hasNext()) {
            return currentIterator.next();
        }

        // 다음 페이지 조회
        List<RealDeal> nextBatch = fetchNextBatch();
        if (nextBatch.isEmpty()) {
            return null;  // 더 이상 데이터 없음
        }

        currentIterator = nextBatch.iterator();
        return currentIterator.hasNext() ? currentIterator.next() : null;
    }

    private List<RealDeal> fetchNextBatch() {
        PageRequest pageRequest = PageRequest.of(0, PAGE_SIZE);  // 항상 첫 페이지 (처리된 건은 상태 변경됨)

        // 1. PENDING 상태 우선 조회
        if (!pendingExhausted) {
            List<RealDeal> pendingDeals = realDealRepository.findByGeoStatus(GeoStatus.PENDING, pageRequest);
            if (!pendingDeals.isEmpty()) {
                return pendingDeals;
            }
            pendingExhausted = true;
        }

        // 2. RETRY 상태 조회 (재시도 횟수 미만)
        return realDealRepository.findByGeoStatusAndRetryCountLessThan(GeoStatus.RETRY, MAX_RETRY, pageRequest);
    }

    /**
     * Step 재시작 시 상태 초기화
     */
    public void resetState() {
        currentIterator = null;
        pendingExhausted = false;
    }
}
