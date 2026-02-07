package com.example.budongbatch.domain.realdeal.step;

import com.example.budongbatch.common.enums.GeoStatus;
import com.example.budongbatch.domain.realdeal.repository.RealDealRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * FAILED 상태 초기화 Step
 *
 * FAILED → RETRY로 변경하여 지오코딩 재시도 대상에 포함
 * retryCount도 0으로 초기화
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ResetFailedStepConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final RealDealRepository realDealRepository;

    @Bean
    public Step resetFailedStep() {
        return new StepBuilder("resetFailedStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    long count = realDealRepository.countByGeoStatus(GeoStatus.FAILED);

                    if (count == 0) {
                        log.info("FAILED 상태 데이터 없음 - 스킵");
                        return RepeatStatus.FINISHED;
                    }

                    int updated = realDealRepository.resetFailedToRetry();
                    log.info("FAILED → RETRY 상태 변경 완료: {}건", updated);

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
