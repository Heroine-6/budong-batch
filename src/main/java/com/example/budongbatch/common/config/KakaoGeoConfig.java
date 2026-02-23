package com.example.budongbatch.common.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * 카카오 Geocoding API Feign 설정
 * - REST API 키를 Authorization 헤더로 전송
 */
public class KakaoGeoConfig {

    @Value("${kakao.map.rest-api-key}")
    private String restApiKey;

    @Bean
    public RequestInterceptor kakaoGeoRequestInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("Authorization", "KakaoAK " + restApiKey);
        };
    }
}
