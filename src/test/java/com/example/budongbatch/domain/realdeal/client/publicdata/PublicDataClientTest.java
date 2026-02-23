package com.example.budongbatch.domain.realdeal.client.publicdata;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PublicDataClientTest {

    @Autowired
    private AptClient aptClient;

    @Autowired
    private OffiClient offiClient;

    @Autowired
    private VillaClient villaClient;

    @Value("${external.api.service-key}")
    private String serviceKey;

    private static final String TEST_LAWD_CD = "11110";  // 서울 종로구
    private static final String TEST_DEAL_YMD = "202501";

    @Test
    @DisplayName("아파트 실거래가 API 호출 테스트")
    void getApt_success() {
        // when
        AptResponse response = aptClient.getApt(serviceKey, TEST_LAWD_CD, TEST_DEAL_YMD, 1, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.response()).isNotNull();
        assertThat(response.response().body()).isNotNull();

        List<AptItem> items = response.response().body().items().item();
        if (items != null && !items.isEmpty()) {
            AptItem first = items.get(0);
            System.out.println("=== 아파트 첫 번째 데이터 ===");
            System.out.println("아파트명: " + first.getName());
            System.out.println("거래금액: " + first.dealAmount());
            System.out.println("전용면적: " + first.excluUseAr());
            System.out.println("거래일: " + first.getDealDate());
            System.out.println("층: " + first.floor());
        }
    }

    @Test
    @DisplayName("오피스텔 실거래가 API 호출 테스트")
    void getOffi_success() {
        // when
        AptResponse response = offiClient.getOffi(serviceKey, TEST_LAWD_CD, TEST_DEAL_YMD, 1, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.response()).isNotNull();

        List<AptItem> items = extractItems(response);
        if (!items.isEmpty()) {
            AptItem first = items.get(0);
            System.out.println("=== 오피스텔 첫 번째 데이터 ===");
            System.out.println("오피스텔명: " + first.getName());
            System.out.println("거래금액: " + first.dealAmount());
            System.out.println("전용면적: " + first.excluUseAr());
            System.out.println("거래일: " + first.getDealDate());
        }
    }

    @Test
    @DisplayName("빌라(연립다세대) 실거래가 API 호출 테스트")
    void getVilla_success() {
        // when
        AptResponse response = villaClient.getVilla(serviceKey, TEST_LAWD_CD, TEST_DEAL_YMD, 1, 10);

        // then
        assertThat(response).isNotNull();
        assertThat(response.response()).isNotNull();

        List<AptItem> items = extractItems(response);
        if (!items.isEmpty()) {
            AptItem first = items.get(0);
            System.out.println("=== 빌라 첫 번째 데이터 ===");
            System.out.println("빌라명: " + first.getName());
            System.out.println("거래금액: " + first.dealAmount());
            System.out.println("전용면적: " + first.excluUseAr());
            System.out.println("거래일: " + first.getDealDate());
        }
    }

    private List<AptItem> extractItems(AptResponse response) {
        if (response == null || response.response() == null
                || response.response().body() == null
                || response.response().body().items() == null
                || response.response().body().items().item() == null) {
            return List.of();
        }
        return response.response().body().items().item();
    }
}
