package com.example.budongbatch.domain.realdeal.step;

import com.example.budongbatch.domain.realdeal.tasklet.EsIndexInitTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * ES 인덱스 초기화 Step 설정
 *
 * indexStep 전에 실행되어 인덱스가 올바른 매핑으로 생성되도록 보장
 */
@Configuration
@RequiredArgsConstructor
public class EsIndexInitStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EsIndexInitTasklet esIndexInitTasklet;

    @Bean
    public Step esIndexInitStep() {
        return new StepBuilder("esIndexInitStep", jobRepository)
                .tasklet(esIndexInitTasklet, transactionManager)
                .build();
    }
}
