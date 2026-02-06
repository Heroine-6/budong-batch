package com.example.budongbatch.domain.realdeal.tasklet;

import com.example.budongbatch.domain.realdeal.document.RealDealDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * ES 인덱스 초기화 Tasklet
 *
 * 인덱스가 없으면 매핑과 설정을 포함하여 생성
 * - @GeoPointField가 geo_point로 매핑되도록 보장
 * - nori_analyzer 설정 적용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexInitTasklet implements Tasklet {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        IndexOperations indexOps = elasticsearchOperations.indexOps(RealDealDocument.class);

        if (!indexOps.exists()) {
            log.info("ES 인덱스 생성 시작: real_deal_search");
            indexOps.createWithMapping();
            log.info("ES 인덱스 생성 완료 (매핑 포함)");
        } else {
            log.info("ES 인덱스 이미 존재: real_deal_search");
        }

        return RepeatStatus.FINISHED;
    }
}
