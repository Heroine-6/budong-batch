package com.example.budongbatch.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 실거래가 파이프라인 스케줄러
 *
 * 매일 02:00 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DealPipelineJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job dealPipelineJob;

    @Scheduled(cron = "0 0 2 * * *")
    public void runDealPipelineJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("runDate", LocalDate.now().toString())
                    .toJobParameters();

            log.info("실거래가 파이프라인 Job 시작");
            jobLauncher.run(dealPipelineJob, jobParameters);
            log.info("실거래가 파이프라인 Job 완료");
        } catch (Exception e) {
            log.error("실거래가 파이프라인 Job 실패", e);
        }
    }
}
