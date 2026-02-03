package com.example.budongbatch.domain.realdeal.client.geocoding;

import com.example.budongbatch.common.config.KakaoGeoConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 카카오 로컬 API Geocoding 클라이언트
 * - 주소 → 좌표(위도/경도) 변환
 * - 네이버 API 실패 시 폴백으로 사용
 * - 일 10만 건 무료
 */
@FeignClient(
        name = "kakaoGeoClient",
        url = "https://dapi.kakao.com/v2/local/search",
        configuration = KakaoGeoConfig.class
)
public interface KakaoGeoClient {

    @GetMapping("/address.json")
    KakaoGeoResponse geocode(@RequestParam("query") String address);
}
