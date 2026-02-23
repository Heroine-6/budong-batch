package com.example.budongbatch.domain.realdeal.partitioner;

import com.example.budongbatch.common.enums.GeoStatus;
import com.example.budongbatch.domain.realdeal.repository.RealDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 지오코딩 대상 데이터를 ID 범위 기반으로 파티셔닝
 *
 * PENDING/RETRY 상태 데이터의 min/max ID를 조회하여
 * gridSize(partition-size) 만큼 균등 분할
 *
 * 예) 10만건, gridSize=4 → 각 파티션 2.5만건씩
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeocodingPartitioner implements Partitioner {

    private final RealDealRepository realDealRepository;

    /**
     * 파티션 생성
     * @param gridSize 파티션 개수 (application.yml의 partition-size)
     * @return 파티션별 ExecutionContext (minId, maxId 포함)
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        // PENDING/RETRY 상태 데이터의 ID 범위 조회
        Long minId = realDealRepository.findMinIdByGeoStatusIn();
        Long maxId = realDealRepository.findMaxIdByGeoStatusIn();

        if (minId == null || maxId == null) {
            log.info("지오코딩 대상 데이터 없음");
            return Map.of();
        }

        // 파티션당 처리할 ID 범위 크기 계산
        long targetSize = (maxId - minId) / gridSize + 1;
        Map<String, ExecutionContext> partitions = new HashMap<>();

        long start = minId;
        long end = start + targetSize - 1;

        // 각 파티션에 ID 범위 할당
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putLong("minId", start);
            context.putLong("maxId", Math.min(end, maxId));

            partitions.put("partition" + i, context);
            log.debug("파티션 {} 생성: {} ~ {}", i, start, Math.min(end, maxId));

            start = end + 1;
            end = start + targetSize - 1;

            if (start > maxId) break;
        }

        log.info("총 {} 파티션 생성 (ID 범위: {} ~ {})", partitions.size(), minId, maxId);
        return partitions;
    }
}
