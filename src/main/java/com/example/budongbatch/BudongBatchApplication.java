package com.example.budongbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 실거래가 데이터 파이프라인 배치 서버
 *
 * API 서버(budong-budong3)와 동일한 DB/ES를 공유하며,
 * 데이터 수집 → 지오코딩 → ES 색인 파이프라인을 담당합니다.
 *
 * [실행 방식]
 * 자동: 매일 02:00 스케줄러 실행 (@Scheduled)
 * 수동: POST /api/batch/run (local/dev 환경)
 *
 * [API 서버와의 관계]
 * 배치 서버: 데이터 수집/가공 (Write)
 * API 서버: 데이터 조회 (Read)
 * 동시 실행 가능, 각자 독립적으로 동작
 *
 * @see com.example.budongbatch.domain.realdeal.job.DealPipelineJobConfig
 * @see com.example.budongbatch.domain.realdeal.scheduler.DealPipelineJobScheduler
 * @see com.example.budongbatch.domain.realdeal.controller.BatchController
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableJpaAuditing
public class BudongBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BudongBatchApplication.class, args);
    }

}
