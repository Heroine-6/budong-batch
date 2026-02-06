package com.example.budongbatch.domain.realdeal.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 실거래가 데이터 파이프라인 Job 설정
 *
 * Job 파라미터
 * runDate: 배치 실행 날짜 (예: 2026-02-05)
 * timestamp: 같은 날 재실행 허용을 위한 타임스탬프
 */
@Configuration
@RequiredArgsConstructor
public class DealPipelineJobConfig {

    private final JobRepository jobRepository;
    private final Step collectStep;
    private final Step geocodeStep;
    private final Step esIndexInitStep;
    private final Step indexStep;

    @Bean
    public Job dealPipelineJob() {
        return new JobBuilder("dealPipelineJob", jobRepository)
                .start(collectStep)
                .next(geocodeStep)
                .next(esIndexInitStep)
                .next(indexStep)
                .build();
    }

    /**
     * ES 재인덱싱 전용 Job
     *
     * SUCCESS 상태 데이터를 ES에 다시 색인할 때 사용
     * - 인덱스 매핑 변경 후 재색인
     * - ES 데이터 유실 시 복구
     */
    @Bean
    public Job reindexJob() {
        return new JobBuilder("reindexJob", jobRepository)
                .start(esIndexInitStep)
                .next(indexStep)
                .build();
    }
}
