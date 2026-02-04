package com.example.budongbatch.domain.realdeal.step;

import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.domain.realdeal.processor.GeocodeProcessor;
import com.example.budongbatch.domain.realdeal.reader.PendingDealReader;
import feign.FeignException;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 지오코딩 Step 설정
 *
 * Chunk 방식 (chunkSize: 100)
 * Reader: PENDING/RETRY 상태 조회
 * Processor: 네이버 → 카카오 폴백 지오코딩
 * Writer: JPA 업데이트
 *
 * Retry/Skip 전략:
 * - retryLimit: 3 (FeignException)
 * - skipLimit: 100 (지오코딩 실패 건)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GeocodeStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final PendingDealReader pendingDealReader;
    private final GeocodeProcessor geocodeProcessor;

    private static final int CHUNK_SIZE = 100;
    private static final int RETRY_LIMIT = 3;
    private static final int SKIP_LIMIT = 100;

    @Bean
    public Step geocodeStep() {
        return new StepBuilder("geocodeStep", jobRepository)
                .<RealDeal, RealDeal>chunk(CHUNK_SIZE, transactionManager)
                .reader(pendingDealReader)
                .processor(geocodeProcessor)
                .writer(realDealWriter())
                .faultTolerant()
                .retryLimit(RETRY_LIMIT)
                .retry(FeignException.class)
                .skipLimit(SKIP_LIMIT)
                .skip(Exception.class)
                .listener(new GeocodeStepListener())
                .build();
    }

    @Bean
    public JpaItemWriter<RealDeal> realDealWriter() {
        return new JpaItemWriterBuilder<RealDeal>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    /**
     * Step 시작/종료 리스너
     */
    @Slf4j
    private static class GeocodeStepListener implements org.springframework.batch.core.StepExecutionListener {
        @Override
        public void beforeStep(org.springframework.batch.core.StepExecution stepExecution) {
            log.info("지오코딩 Step 시작");
        }

        @Override
        public org.springframework.batch.core.ExitStatus afterStep(org.springframework.batch.core.StepExecution stepExecution) {
            log.info("지오코딩 Step 완료 - 읽음: {}, 처리: {}, 스킵: {}",
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getSkipCount());
            return stepExecution.getExitStatus();
        }
    }
}
