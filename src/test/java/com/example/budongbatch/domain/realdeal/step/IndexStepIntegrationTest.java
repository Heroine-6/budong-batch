package com.example.budongbatch.domain.realdeal.step;

import com.example.budongbatch.domain.realdeal.document.RealDealDocument;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.domain.realdeal.repository.RealDealRepository;
import com.example.budongbatch.fixture.RealDealFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class IndexStepIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private RealDealRepository realDealRepository;

    @MockBean
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private Job dealPipelineJob;

    @BeforeEach
    void setUp() {
        realDealRepository.deleteAll();
        jobLauncherTestUtils.setJob(dealPipelineJob);
    }

    @Test
    @DisplayName("indexStep - SUCCESS 상태 데이터를 ES에 색인한다")
    void indexStep_indexesSuccessDeals() throws Exception {
        // given: PENDING 상태로 저장 후 SUCCESS로 전환
        RealDeal deal1 = RealDealFixture.pending();
        RealDeal deal2 = RealDealFixture.pending();
        List<RealDeal> saved = realDealRepository.saveAllAndFlush(List.of(deal1, deal2));

        // 저장된 엔티티를 SUCCESS 상태로 변경
        saved.get(0).applyGeoCode(new java.math.BigDecimal("37.5012743"), new java.math.BigDecimal("127.0396597"), "서울특별시 강남구 역삼로 123");
        saved.get(1).applyGeoCode(new java.math.BigDecimal("37.5012743"), new java.math.BigDecimal("127.0396597"), "서울특별시 강남구 역삼로 456");
        realDealRepository.saveAllAndFlush(saved);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("runDate", "2026-02-05")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution execution = jobLauncherTestUtils.launchStep("indexStep", jobParameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        verify(elasticsearchOperations, atLeastOnce()).bulkIndex(any(List.class), eq(RealDealDocument.class));
    }

    @Test
    @DisplayName("indexStep - SUCCESS 상태 데이터가 없으면 색인하지 않는다")
    void indexStep_noData_skipsIndexing() throws Exception {
        // given
        RealDeal pendingDeal = RealDealFixture.pending();
        realDealRepository.save(pendingDeal);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("runDate", "2026-02-05")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution execution = jobLauncherTestUtils.launchStep("indexStep", jobParameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getStepExecutions().iterator().next().getReadCount()).isZero();
    }
}
