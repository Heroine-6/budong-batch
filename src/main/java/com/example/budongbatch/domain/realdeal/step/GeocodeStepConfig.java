package com.example.budongbatch.domain.realdeal.step;

import com.example.budongbatch.common.config.BatchProperties;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.domain.realdeal.partitioner.GeocodingPartitioner;
import com.example.budongbatch.domain.realdeal.processor.GeocodeProcessor;
import com.example.budongbatch.domain.realdeal.reader.PartitionedPendingDealReader;
import com.example.budongbatch.domain.realdeal.repository.RealDealRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 지오코딩 Step 설정 (파티셔닝 방식)
 *
 * Master Step: 데이터를 ID 범위로 파티셔닝
 * Worker Step: 각 파티션을 독립적으로 처리
 *
 * 장점:
 * - 각 파티션이 독립된 Reader → 동시성 이슈 없음
 * - 외부 API 호출 병렬화
 * - 실패 파티션만 재시도 가능
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GeocodeStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final RealDealRepository realDealRepository;
    private final GeocodeProcessor geocodeProcessor;
    private final GeocodingPartitioner geocodingPartitioner;
    private final BatchProperties batchProperties;

    /**
     * Master Step - 파티셔닝 관리
     *
     * 역할:
     * 1. Partitioner를 통해 데이터를 ID 범위로 분할
     * 2. PartitionHandler에게 Worker Step 실행 위임
     * 3. 모든 파티션 완료 대기 후 다음 Step 진행
     *
     * @param partitionHandler Worker Step 병렬 실행 담당
     */
    @Bean
    public Step geocodeStep(TaskExecutorPartitionHandler partitionHandler) {
        return new StepBuilder("geocodeStep", jobRepository)
                .partitioner("geocodeWorkerStep", geocodingPartitioner)
                .partitionHandler(partitionHandler)
                .build();
    }

    /**
     * Partition Handler - 파티션별 Step 실행 관리
     *
     * 설정:
     * - step: 각 파티션에서 실행할 Worker Step
     * - taskExecutor: 병렬 실행용 Thread Pool
     * - gridSize: 파티션 개수 (application.yml의 partition-size)
     *
     * @param geocodeWorkerStep Worker Step (파티션별 실행)
     */
    @Bean
    public TaskExecutorPartitionHandler partitionHandler(Step geocodeWorkerStep) {
        TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();
        handler.setStep(geocodeWorkerStep);
        handler.setTaskExecutor(geocodeTaskExecutor());
        handler.setGridSize(batchProperties.getGeocode().getPartitionSize());
        return handler;
    }

    /**
     * Worker Step - 실제 지오코딩 처리
     *
     * 각 파티션(스레드)에서 독립적으로 실행
     * 처리 흐름: Reader → Processor → Writer (chunk 단위)
     *
     * @param partitionedPendingDealReader StepScope Bean - 파티션별 독립 인스턴스 (stepExecutionContext에서 minId, maxId 주입)
     */
    @Bean
    public Step geocodeWorkerStep(ItemReader<RealDeal> partitionedPendingDealReader) {
        return new StepBuilder("geocodeWorkerStep", jobRepository)
                .<RealDeal, RealDeal>chunk(batchProperties.getGeocode().getChunkSize(), transactionManager)
                .reader(partitionedPendingDealReader)
                .processor(geocodeProcessor)
                .writer(realDealWriter())
                .listener(new GeocodeStepListener())
                .build();
    }

    /**
     * 파티션별 Reader
     *
     * @StepScope: 각 파티션 실행 시점에 새 인스턴스 생성
     * SpEL(#{stepExecutionContext['key']}): Partitioner가 설정한 값 주입
     *
     * @param minId Partitioner가 할당한 시작 ID
     * @param maxId Partitioner가 할당한 종료 ID
     */
    @Bean
    @StepScope
    public PartitionedPendingDealReader partitionedPendingDealReader(
            @Value("#{stepExecutionContext['minId']}") Long minId,
            @Value("#{stepExecutionContext['maxId']}") Long maxId) {
        return new PartitionedPendingDealReader(realDealRepository, batchProperties, minId, maxId);
    }

    /**
     * 지오코딩용 Thread Pool
     *
     * partition-size=4일 때:
     * - corePoolSize=4: 기본 4개 스레드
     * - maxPoolSize=4: 최대 4개 스레드 (고정)
     * - threadNamePrefix: 로그에서 "geocode-1", "geocode-2" 등으로 표시
     */
    @Bean
    public TaskExecutor geocodeTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(batchProperties.getGeocode().getPartitionSize());
        executor.setMaxPoolSize(batchProperties.getGeocode().getPartitionSize());
        executor.setThreadNamePrefix("geocode-");
        executor.initialize();
        return executor;
    }

    /**
     * JPA Writer - 지오코딩 결과 DB 저장
     */
    @Bean
    public JpaItemWriter<RealDeal> realDealWriter() {
        return new JpaItemWriterBuilder<RealDeal>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    /**
     * Worker Step 모니터링용 Listener
     * 각 파티션별 처리 현황 로깅
     */
    @Slf4j
    private static class GeocodeStepListener implements org.springframework.batch.core.StepExecutionListener {
        @Override
        public void beforeStep(org.springframework.batch.core.StepExecution stepExecution) {
            log.info("지오코딩 Worker Step 시작 - {}", stepExecution.getStepName());
        }

        @Override
        public org.springframework.batch.core.ExitStatus afterStep(org.springframework.batch.core.StepExecution stepExecution) {
            log.info("지오코딩 Worker Step 완료 - 읽음: {}, 처리: {}, 스킵: {}",
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getSkipCount());
            return stepExecution.getExitStatus();
        }
    }
}
