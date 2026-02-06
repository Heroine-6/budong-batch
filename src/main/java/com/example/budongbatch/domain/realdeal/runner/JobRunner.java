package com.example.budongbatch.domain.realdeal.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 커맨드라인 Job 실행기
 * 초기 적재/재인덱싱을 빠르게 실행하려고 만든 유틸성 런너
 *
 * 사용법:
 * Makefile
 *
 * batch.job.name이 없으면 아무 Job도 실행하지 않음 (스케줄러만 동작)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobRunner implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final ApplicationContext applicationContext;

    @Value("${batch.job.name:}")
    private String jobName;

    @Value("${batch.runDate:}")
    private String runDate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (jobName == null || jobName.isBlank()) {
            log.info("batch.job.name 미지정 - 스케줄러 대기 모드");
            return;
        }

        try {
            Job job = applicationContext.getBean(jobName, Job.class);

            LocalDate targetDate = parseRunDate(runDate);
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("runDate", targetDate.toString())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            log.info("Job 실행: {} (runDate: {})", jobName, targetDate);
            jobLauncher.run(job, jobParameters);
            log.info("Job 완료: {}", jobName);
        } catch (Exception e) {
            log.error("Job 실행 실패: {}", jobName, e);
            throw e;
        }
    }

    private LocalDate parseRunDate(String runDate) {
        if (runDate == null || runDate.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(runDate);
        } catch (Exception e) {
            log.warn("runDate 파싱 실패, 오늘 날짜 사용: {}", runDate);
            return LocalDate.now();
        }
    }
}
