package com.example.budongbatch.domain.realdeal.scheduler;

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
 * [실행 시간]
 * 매일 02:00 (KST) - 공공데이터 API 트래픽이 적은 새벽 시간대
 *
 * [수동 실행]
 * 개발/테스트 환경에서는 BatchController를 통해 수동 실행 가능
 * POST /api/v2batch/run
 * POST /api/v2/batch/run?runDate=2026-01-15  (특정 날짜 재처리)
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
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            log.info("실거래가 파이프라인 Job 시작");
            jobLauncher.run(dealPipelineJob, jobParameters);
            log.info("실거래가 파이프라인 Job 완료");
        } catch (Exception e) {
            log.error("실거래가 파이프라인 Job 실패", e);
        }
    }
}
