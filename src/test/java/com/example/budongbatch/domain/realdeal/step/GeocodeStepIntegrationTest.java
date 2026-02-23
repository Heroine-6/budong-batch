package com.example.budongbatch.domain.realdeal.step;

import com.example.budongbatch.common.enums.GeoStatus;
import com.example.budongbatch.domain.realdeal.client.geocoding.KakaoGeoClient;
import com.example.budongbatch.domain.realdeal.client.geocoding.KakaoGeoResponse;
import com.example.budongbatch.domain.realdeal.client.geocoding.NaverGeoClient;
import com.example.budongbatch.domain.realdeal.client.geocoding.NaverGeoResponse;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class GeocodeStepIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private RealDealRepository realDealRepository;

    @MockBean
    private NaverGeoClient naverGeoClient;

    @MockBean
    private KakaoGeoClient kakaoGeoClient;

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
    @DisplayName("geocodeStep - PENDING 데이터를 지오코딩하여 SUCCESS로 변경한다")
    void geocodeStep_geocodesPendingDeals() throws Exception {
        // given
        RealDeal pendingDeal = RealDealFixture.pending();
        realDealRepository.save(pendingDeal);

        NaverGeoResponse.Address address = new NaverGeoResponse.Address(
                "서울특별시 강남구 역삼로 123",
                "서울특별시 강남구 역삼동 123-45",
                "127.0396597",
                "37.5012743"
        );
        when(naverGeoClient.geocode(anyString()))
                .thenReturn(new NaverGeoResponse("OK", new NaverGeoResponse.Meta(1), List.of(address)));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("runDate", "2026-02-05")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution execution = jobLauncherTestUtils.launchStep("geocodeStep", jobParameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        RealDeal updated = realDealRepository.findAll().get(0);
        assertThat(updated.getGeoStatus()).isEqualTo(GeoStatus.SUCCESS);
        assertThat(updated.getLatitude()).isNotNull();
        assertThat(updated.getLongitude()).isNotNull();
    }

    @Test
    @DisplayName("geocodeStep - 네이버 실패 시 카카오로 폴백한다")
    void geocodeStep_fallbackToKakao() throws Exception {
        // given
        RealDeal pendingDeal = RealDealFixture.pending();
        realDealRepository.save(pendingDeal);

        when(naverGeoClient.geocode(anyString()))
                .thenReturn(new NaverGeoResponse("OK", new NaverGeoResponse.Meta(0), List.of()));

        KakaoGeoResponse.Document doc = new KakaoGeoResponse.Document(
                "서울특별시 강남구 역삼동",
                "127.0396597",
                "37.5012743"
        );
        when(kakaoGeoClient.geocode(anyString()))
                .thenReturn(new KakaoGeoResponse(List.of(doc)));

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("runDate", "2026-02-05")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution execution = jobLauncherTestUtils.launchStep("geocodeStep", jobParameters);

        // then
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        RealDeal updated = realDealRepository.findAll().get(0);
        assertThat(updated.getGeoStatus()).isEqualTo(GeoStatus.SUCCESS);
    }
}
