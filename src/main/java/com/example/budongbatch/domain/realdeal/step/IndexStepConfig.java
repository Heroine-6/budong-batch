package com.example.budongbatch.domain.realdeal.step;

import com.example.budongbatch.common.constants.BatchConstants;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.domain.realdeal.reader.SuccessDealReader;
import com.example.budongbatch.domain.realdeal.writer.EsIndexWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * ES 색인 Step 설정
 *
 * Chunk 방식
 * Reader: geoStatus = SUCCESS 조회
 * Writer: ElasticsearchOperations 벌크 색인
 *
 * GeoPoint 필드로 위치 기반 검색 지원
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class IndexStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SuccessDealReader successDealReader;
    private final EsIndexWriter esIndexWriter;

    @Bean
    public Step indexStep() {
        return new StepBuilder("indexStep", jobRepository)
                .<RealDeal, RealDeal>chunk(BatchConstants.INDEX_CHUNK_SIZE, transactionManager)
                .reader(successDealReader)
                .writer(esIndexWriter)
                .listener(new IndexStepListener())
                .build();
    }

    @Slf4j
    private static class IndexStepListener implements StepExecutionListener {
        @Override
        public void beforeStep(StepExecution stepExecution) {
            log.info("ES 색인 Step 시작");
        }

        @Override
        public ExitStatus afterStep(StepExecution stepExecution) {
            log.info("ES 색인 Step 완료 - 읽음: {}, 색인: {}, 스킵: {}",
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getSkipCount());
            return stepExecution.getExitStatus();
        }
    }
}
