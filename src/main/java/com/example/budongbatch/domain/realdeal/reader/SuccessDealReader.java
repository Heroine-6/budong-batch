package com.example.budongbatch.domain.realdeal.reader;

import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.domain.realdeal.repository.RealDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * ES 색인 대상 Reader
 *
 * geoStatus = SUCCESS인 데이터 조회
 * ID 기반 커서 방식으로 페이지네이션하여 메모리 효율성 확보
 */
@StepScope
@Component
@RequiredArgsConstructor
public class SuccessDealReader implements ItemStreamReader<RealDeal> {

    private final RealDealRepository realDealRepository;

    private static final int PAGE_SIZE = 500;
    private static final String LAST_PROCESSED_ID_KEY = "indexStep.lastProcessedId";

    private Iterator<RealDeal> currentIterator;
    private Long lastProcessedId = 0L;

    @Override
    public void open(ExecutionContext executionContext) {
        if (executionContext.containsKey(LAST_PROCESSED_ID_KEY)) {
            this.lastProcessedId = executionContext.getLong(LAST_PROCESSED_ID_KEY);
        }
    }

    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putLong(LAST_PROCESSED_ID_KEY, this.lastProcessedId);
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public RealDeal read() {
        if (currentIterator != null && currentIterator.hasNext()) {
            RealDeal deal = currentIterator.next();
            lastProcessedId = deal.getId();
            return deal;
        }

        List<RealDeal> nextBatch = fetchNextBatch();
        if (nextBatch.isEmpty()) {
            return null;
        }

        currentIterator = nextBatch.iterator();
        if (currentIterator.hasNext()) {
            RealDeal deal = currentIterator.next();
            lastProcessedId = deal.getId();
            return deal;
        }
        return null;
    }

    private List<RealDeal> fetchNextBatch() {
        return realDealRepository.findGeoCodedAfter(lastProcessedId, PageRequest.of(0, PAGE_SIZE));
    }
}
