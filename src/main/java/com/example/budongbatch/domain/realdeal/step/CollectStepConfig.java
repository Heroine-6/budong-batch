package com.example.budongbatch.domain.realdeal.step;

import com.example.budongbatch.domain.realdeal.tasklet.CollectTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class CollectStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CollectTasklet collectTasklet;

    @Bean
    public Step collectStep() {
        return new StepBuilder("collectStep", jobRepository)
                .tasklet(collectTasklet, transactionManager)
                .build();
    }
}
