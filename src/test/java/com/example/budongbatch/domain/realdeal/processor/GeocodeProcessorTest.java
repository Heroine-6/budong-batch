package com.example.budongbatch.domain.realdeal.processor;

import com.example.budongbatch.common.enums.GeoStatus;
import com.example.budongbatch.domain.realdeal.client.geocoding.KakaoGeoClient;
import com.example.budongbatch.domain.realdeal.client.geocoding.KakaoGeoResponse;
import com.example.budongbatch.domain.realdeal.client.geocoding.NaverGeoClient;
import com.example.budongbatch.domain.realdeal.client.geocoding.NaverGeoResponse;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.fixture.RealDealFixture;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GeocodeProcessorTest {

    @Mock
    private NaverGeoClient naverGeoClient;

    @Mock
    private KakaoGeoClient kakaoGeoClient;

    private GeocodeProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new GeocodeProcessor(naverGeoClient, kakaoGeoClient);
    }

    @Nested
    @DisplayName("process() - 지오코딩 처리")
    class ProcessTest {

        @Test
        @DisplayName("네이버 지오코딩 성공")
        void process_naverSuccess() {
            // given
            RealDeal deal = RealDealFixture.pending();
            NaverGeoResponse response = naverResponse("37.5012743", "127.0396597", "서울특별시 강남구 역삼로 123");

            given(naverGeoClient.geocode(anyString())).willReturn(response);

            // when
            RealDeal result = processor.process(deal);

            // then
            assertThat(result.getGeoStatus()).isEqualTo(GeoStatus.SUCCESS);
            assertThat(result.getLatitude()).isEqualByComparingTo(new BigDecimal("37.5012743"));
            assertThat(result.getLongitude()).isEqualByComparingTo(new BigDecimal("127.0396597"));
            assertThat(result.getRoadAddress()).isEqualTo("서울특별시 강남구 역삼로 123");

            verify(kakaoGeoClient, never()).geocode(anyString());  // 카카오 호출 안 함
        }

        @Test
        @DisplayName("네이버 실패 → 카카오 성공 (폴백)")
        void process_naverFail_kakaoSuccess() {
            // given
            RealDeal deal = RealDealFixture.pending();
            NaverGeoResponse emptyNaverResponse = naverResponse(null, null, null);
            KakaoGeoResponse kakaoResponse = kakaoResponse("37.5012743", "127.0396597", "서울특별시 강남구 역삼동 123-45");

            given(naverGeoClient.geocode(anyString())).willReturn(emptyNaverResponse);
            given(kakaoGeoClient.geocode(anyString())).willReturn(kakaoResponse);

            // when
            RealDeal result = processor.process(deal);

            // then
            assertThat(result.getGeoStatus()).isEqualTo(GeoStatus.SUCCESS);
            assertThat(result.getLatitude()).isEqualByComparingTo(new BigDecimal("37.5012743"));
            assertThat(result.getLongitude()).isEqualByComparingTo(new BigDecimal("127.0396597"));
        }

        @Test
        @DisplayName("네이버 예외 → 카카오 성공 (폴백)")
        void process_naverException_kakaoSuccess() {
            // given
            RealDeal deal = RealDealFixture.pending();
            KakaoGeoResponse kakaoResponse = kakaoResponse("37.5012743", "127.0396597", "서울특별시 강남구 역삼동");

            given(naverGeoClient.geocode(anyString())).willThrow(FeignException.class);
            given(kakaoGeoClient.geocode(anyString())).willReturn(kakaoResponse);

            // when
            RealDeal result = processor.process(deal);

            // then
            assertThat(result.getGeoStatus()).isEqualTo(GeoStatus.SUCCESS);
        }

        @Test
        @DisplayName("둘 다 실패 → RETRY 상태")
        void process_bothFail_retry() {
            // given
            RealDeal deal = RealDealFixture.pending();

            given(naverGeoClient.geocode(anyString())).willThrow(FeignException.class);
            given(kakaoGeoClient.geocode(anyString())).willThrow(FeignException.class);

            // when
            RealDeal result = processor.process(deal);

            // then
            assertThat(result.getGeoStatus()).isEqualTo(GeoStatus.RETRY);
            assertThat(result.getRetryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("재시도 횟수 초과 → FAILED 상태")
        void process_maxRetryExceeded_failed() {
            // given
            RealDeal deal = RealDealFixture.retry(2);  // 이미 2회 재시도

            given(naverGeoClient.geocode(anyString())).willThrow(FeignException.class);
            given(kakaoGeoClient.geocode(anyString())).willThrow(FeignException.class);

            // when
            RealDeal result = processor.process(deal);

            // then
            assertThat(result.getGeoStatus()).isEqualTo(GeoStatus.FAILED);
        }

        @Test
        @DisplayName("네이버 결과 없음 + 카카오 결과 없음 → RETRY")
        void process_noResults_retry() {
            // given
            RealDeal deal = RealDealFixture.pending();
            NaverGeoResponse emptyNaver = new NaverGeoResponse("OK", new NaverGeoResponse.Meta(0), List.of());
            KakaoGeoResponse emptyKakao = new KakaoGeoResponse(List.of());

            given(naverGeoClient.geocode(anyString())).willReturn(emptyNaver);
            given(kakaoGeoClient.geocode(anyString())).willReturn(emptyKakao);

            // when
            RealDeal result = processor.process(deal);

            // then
            assertThat(result.getGeoStatus()).isEqualTo(GeoStatus.RETRY);
        }
    }

    private NaverGeoResponse naverResponse(String y, String x, String roadAddress) {
        if (y == null) {
            return new NaverGeoResponse("OK", new NaverGeoResponse.Meta(0), List.of());
        }
        return new NaverGeoResponse(
                "OK",
                new NaverGeoResponse.Meta(1),
                List.of(new NaverGeoResponse.Address(roadAddress, "지번주소", x, y))
        );
    }

    private KakaoGeoResponse kakaoResponse(String y, String x, String addressName) {
        if (y == null) {
            return new KakaoGeoResponse(List.of());
        }
        return new KakaoGeoResponse(
                List.of(new KakaoGeoResponse.Document(addressName, x, y))
        );
    }
}
