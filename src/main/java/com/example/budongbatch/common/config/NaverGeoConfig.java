package com.example.budongbatch.common.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * 네이버 Geocoding API Feign 설정
 * - NCloud API Gateway 인증 헤더 추가
 * - Referer 헤더로 등록된 웹 서비스 URL 전송
 */
public class NaverGeoConfig {

    @Value("${naver.map.client-id}")
    private String clientId;

    @Value("${naver.map.client-secret}")
    private String clientSecret;

    @Bean
    public RequestInterceptor naverGeoRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-NCP-APIGW-API-KEY-ID", clientId);
            requestTemplate.header("X-NCP-APIGW-API-KEY", clientSecret);
            requestTemplate.header("Referer", "https://cattlejuyawn.tistory.com/");
        };
    }
}
