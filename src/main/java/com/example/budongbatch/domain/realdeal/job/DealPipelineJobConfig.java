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
 * 파이프라인: collectStep → geocodeStep → indexStep
 * 공공데이터, 지오코딩 따로 돌리고싶으면 별도 job필요
 */
@Configuration
@RequiredArgsConstructor
public class DealPipelineJobConfig {

    private final JobRepository jobRepository;
    private final Step collectStep;
    private final Step geocodeStep;
    private final Step indexStep;

    @Bean
    public Job dealPipelineJob() {
        return new JobBuilder("dealPipelineJob", jobRepository)
                .start(collectStep)
                .next(geocodeStep)
                .next(indexStep)
                .build();
    }
}
