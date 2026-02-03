package com.example.budongbatch.domain.realdeal.client.geocoding;

import com.example.budongbatch.common.config.NaverGeoConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 네이버 클라우드 플랫폼 Geocoding API 클라이언트
 * - 주소 → 좌표(위도/경도) 변환
 * - 월 300만 건 무료
 */
@FeignClient(
        name = "naverGeoClient",
        url = "https://maps.apigw.ntruss.com/map-geocode/v2",
        configuration = NaverGeoConfig.class
)
public interface NaverGeoClient {

    @GetMapping("/geocode")
    NaverGeoResponse geocode(@RequestParam("query") String address);
}
