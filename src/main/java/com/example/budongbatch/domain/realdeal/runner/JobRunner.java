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
 *
 * 파라미터 우선순위 (수집 대상 월 결정):
 * 1. dealYmd (직접 지정, 예: 202512)
 * 2. runDate (날짜에서 추출, 예: 2025-12-01 → 202512)
 * 3. 기본값: 전월
 *
 * 사용 예시:
 * --batch.job.name=dealPipelineJob --batch.dealYmd=202512
 * --batch.job.name=geocodeRetryJob
 * --batch.job.name=reindexJob
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

    @Value("${batch.dealYmd:}")
    private String dealYmd;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (jobName == null || jobName.isBlank()) {
            log.info("batch.job.name 미지정 - 스케줄러 대기 모드");
            return;
        }

        try {
            Job job = applicationContext.getBean(jobName, Job.class);

            LocalDate targetDate = parseRunDate(runDate);
            JobParametersBuilder builder = new JobParametersBuilder()
                    .addString("runDate", targetDate.toString())
                    .addLong("timestamp", System.currentTimeMillis());

            // dealYmd 직접 지정 시 추가 (예: 202512)
            if (dealYmd != null && !dealYmd.isBlank()) {
                if (!isValidDealYmd(dealYmd)) {
                    log.warn("dealYmd 형식 오류 (예: 202512): {} - 무시하고 runDate 사용", dealYmd);
                } else {
                    builder.addString("dealYmd", dealYmd);
                    log.info("Job 실행: {} (dealYmd: {})", jobName, dealYmd);
                }
            }

            if (dealYmd == null || dealYmd.isBlank() || !isValidDealYmd(dealYmd)) {
                log.info("Job 실행: {} (runDate: {} → {}월)", jobName, targetDate,
                        targetDate.getYear() + String.format("%02d", targetDate.getMonthValue()));
            }

            jobLauncher.run(job, builder.toJobParameters());
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

    /**
     * dealYmd 형식 검증 (YYYYMM, 예: 202512)
     */
    private boolean isValidDealYmd(String dealYmd) {
        if (!dealYmd.matches("\\d{6}")) {
            return false;
        }
        int month = Integer.parseInt(dealYmd.substring(4, 6));
        return month >= 1 && month <= 12;
    }
}
