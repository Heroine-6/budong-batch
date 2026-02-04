package com.example.budongbatch.domain.realdeal.processor;

import com.example.budongbatch.domain.realdeal.client.geocoding.KakaoGeoClient;
import com.example.budongbatch.domain.realdeal.client.geocoding.KakaoGeoResponse;
import com.example.budongbatch.domain.realdeal.client.geocoding.NaverGeoClient;
import com.example.budongbatch.domain.realdeal.client.geocoding.NaverGeoResponse;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 지오코딩 Processor
 *
 * 네이버 지오코딩 API 우선 호출
 * 실패 시 카카오 API로 폴백
 * 둘 다 실패 시 RETRY/FAILED 상태로 변경
 */
@Slf4j
@Component
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class GeocodeProcessor implements ItemProcessor<RealDeal, RealDeal> {

    private final NaverGeoClient naverGeoClient;
    private final KakaoGeoClient kakaoGeoClient;

    private static final int MAX_RETRY = 3;

    @Override
    public RealDeal process(RealDeal item) {
        String address = item.getAddress();

        // 1. 네이버 지오코딩 시도
        GeoResult naverResult = tryNaverGeocode(address);
        if (naverResult != null) {
            item.applyGeoCode(naverResult.latitude, naverResult.longitude, naverResult.roadAddress);
            return item;
        }

        // 2. 카카오 지오코딩 폴백
        GeoResult kakaoResult = tryKakaoGeocode(address);
        if (kakaoResult != null) {
            item.applyGeoCode(kakaoResult.latitude, kakaoResult.longitude, kakaoResult.roadAddress);
            return item;
        }

        // 3. 둘 다 실패
        handleGeocodingFailure(item);
        return item;
    }

    private GeoResult tryNaverGeocode(String address) {
        try {
            NaverGeoResponse response = naverGeoClient.geocode(address);
            if (response.hasResult()) {
                NaverGeoResponse.Address addr = response.addresses().get(0);
                return new GeoResult(
                        new BigDecimal(addr.y()),  // 위도
                        new BigDecimal(addr.x()),  // 경도
                        addr.roadAddress()
                );
            }
        } catch (Exception e) {
            log.warn("네이버 지오코딩 실패 - address: {}, error: {}", address, e.getMessage());
        }
        return null;
    }

    private GeoResult tryKakaoGeocode(String address) {
        try {
            KakaoGeoResponse response = kakaoGeoClient.geocode(address);
            if (response.hasResult()) {
                KakaoGeoResponse.Document doc = response.documents().get(0);
                return new GeoResult(
                        new BigDecimal(doc.y()),  // 위도
                        new BigDecimal(doc.x()),  // 경도
                        doc.addressName()
                );
            }
        } catch (Exception e) {
            log.warn("카카오 지오코딩 실패 - address: {}, error: {}", address, e.getMessage());
        }
        return null;
    }

    private void handleGeocodingFailure(RealDeal item) {
        if (item.getRetryCount() >= MAX_RETRY - 1) {
            item.markGeoFailed(true);  // 영구 실패
            log.warn("지오코딩 최종 실패 (FAILED): address={}, retryCount={}",
                    item.getAddress(), item.getRetryCount());
        } else {
            item.markGeoFailed(false);  // 재시도 대상
            log.debug("지오코딩 실패 (RETRY): address={}, retryCount={}",
                    item.getAddress(), item.getRetryCount());
        }
    }

    private record GeoResult(BigDecimal latitude, BigDecimal longitude, String roadAddress) {}
}
