package com.example.budongbatch.domain.realdeal.reader;

import com.example.budongbatch.common.config.BatchProperties;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.domain.realdeal.repository.RealDealRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * 파티션별 지오코딩 대상 Reader
 *
 * 각 파티션은 독립된 ID 범위(minId~maxId)를 처리
 * StepScope로 생성되어 파티션별 독립 인스턴스 보장
 *
 * 동작 방식:
 * 1. stepExecutionContext에서 minId, maxId 주입받음
 * 2. 해당 ID 범위 내 PENDING/RETRY 상태 데이터 조회
 * 3. 커서 기반 페이징으로 메모리 효율적 처리
 */
@Slf4j
public class PartitionedPendingDealReader implements ItemReader<RealDeal> {

    private final RealDealRepository realDealRepository;
    private final BatchProperties batchProperties;

    //파티션 ID 범위 (Partitioner에서 할당)
    private final Long minId;
    private final Long maxId;

    private Iterator<RealDeal> currentIterator;
    // 마지막 처리된 ID (커서 기반 페이징용)
    private Long lastProcessedId;

    public PartitionedPendingDealReader(
            RealDealRepository realDealRepository,
            BatchProperties batchProperties,
            Long minId,
            Long maxId) {
        this.realDealRepository = realDealRepository;
        this.batchProperties = batchProperties;
        this.minId = minId;
        this.maxId = maxId;
        this.lastProcessedId = minId - 1;

        log.debug("파티션 Reader 초기화: ID 범위 {} ~ {}", minId, maxId);
    }

    @Override
    public RealDeal read() {
        // 현재 배치에 데이터 있으면 반환
        if (currentIterator != null && currentIterator.hasNext()) {
            RealDeal deal = currentIterator.next();
            lastProcessedId = deal.getId();
            return deal;
        }

        // 다음 배치 조회
        List<RealDeal> nextBatch = fetchNextBatch();
        if (nextBatch.isEmpty()) {
            return null;  // 파티션 처리 완료
        }

        currentIterator = nextBatch.iterator();
        if (currentIterator.hasNext()) {
            RealDeal deal = currentIterator.next();
            lastProcessedId = deal.getId();
            return deal;
        }
        return null;
    }

    /**
     * ID 범위 내 PENDING/RETRY 상태 데이터 조회
     * 커서 기반: lastProcessedId 이후 데이터만 조회
     */
    private List<RealDeal> fetchNextBatch() {
        int pageSize = batchProperties.getGeocode().getPageSize();
        int maxRetry = batchProperties.getGeocode().getMaxRetry();

        return realDealRepository.findByIdRangeAndGeoStatus(
                lastProcessedId + 1,
                maxId,
                maxRetry,
                PageRequest.of(0, pageSize)
        );
    }
}
