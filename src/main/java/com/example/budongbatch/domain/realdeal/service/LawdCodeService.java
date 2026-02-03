package com.example.budongbatch.domain.realdeal.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class LawdCodeService {

    private final Map<String, String> addressToCodeMap = new HashMap<>();
    private final Map<String, String> codeToAddressMap = new HashMap<>();

    @PostConstruct
    public void init() {
        loadLawdCodes();
    }

    private void loadLawdCodes() {
        try {
            ClassPathResource resource = new ClassPathResource("국토교통부_전국 법정동_20250807.csv");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                reader.readLine(); // 헤더 스킵

                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", -1);
                    if (parts.length < 5) continue;

                    String code = parts[0].trim();
                    String sido = parts[1].trim();
                    String sigungu = parts[2].trim();
                    String eupmyeondong = parts[3].trim();

                    // 삭제된 코드는 제외 (삭제일자가 있으면)
                    if (parts.length > 7 && !parts[7].trim().isEmpty()) continue;

                    // 5자리 지역코드 추출 (API용)
                    String lawdCd = code.substring(0, 5);

                    // 코드 → 주소 역방향 매핑 (시도 + 시군구)
                    if (!sigungu.isEmpty()) {
                        String prefix = sido + " " + sigungu;
                        codeToAddressMap.putIfAbsent(lawdCd, prefix);
                    }

                    // 다양한 주소 패턴으로 매핑
                    if (!eupmyeondong.isEmpty()) {
                        // 시도 + 시군구 + 읍면동
                        String fullAddress = sido + " " + sigungu + " " + eupmyeondong;
                        addressToCodeMap.putIfAbsent(fullAddress, lawdCd);

                        // 시군구 + 읍면동
                        String shortAddress = sigungu + " " + eupmyeondong;
                        addressToCodeMap.putIfAbsent(shortAddress, lawdCd);
                    } else if (!sigungu.isEmpty()) {
                        // 시도 + 시군구
                        String address = sido + " " + sigungu;
                        addressToCodeMap.putIfAbsent(address, lawdCd);
                        addressToCodeMap.putIfAbsent(sigungu, lawdCd);
                    }
                }

                log.info("법정동 코드 로드 완료: {}건", addressToCodeMap.size());
            }
        } catch (Exception e) {
            log.error("법정동 코드 로드 실패", e);
        }
    }

    /**
     * 지역코드에서 주소 prefix 추출 (시도 + 시군구)
     * @param lawdCd 5자리 지역코드
     * @return 시도 + 시군구 (예: "서울특별시 종로구")
     */
    public Optional<String> getAddressPrefixFromLawdCd(String lawdCd) {
        if (lawdCd == null || lawdCd.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(codeToAddressMap.get(lawdCd));
    }

    /**
     * 모든 법정동 코드 목록 반환
     */
    public List<String> getAllLawdCodes() {
        return new ArrayList<>(new HashSet<>(addressToCodeMap.values()));
    }

    /**
     * 주소에서 지역코드(LAWD_CD) 추출
     * @param address 사용자가 입력한 주소
     * @return 5자리 지역코드
     */
    public Optional<String> getLawdCdFromAddress(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }

        // 1. 정확히 일치하는 경우
        if (addressToCodeMap.containsKey(address)) {
            return Optional.of(addressToCodeMap.get(address));
        }

        // 2. 주소에 포함된 지역명으로 검색 (긴 키부터 매칭 - 더 구체적인 주소 우선)
        String bestMatch = null;
        String bestCode = null;
        for (Map.Entry<String, String> entry : addressToCodeMap.entrySet()) {
            String key = entry.getKey();
            if (address.contains(key)) {
                if (bestMatch == null || key.length() > bestMatch.length()) {
                    bestMatch = key;
                    bestCode = entry.getValue();
                }
            }
        }

        if (bestCode != null) {
            return Optional.of(bestCode);
        }

        // 3. 주소를 공백으로 분리해서 검색
        String[] parts = address.split("\\s+");
        for (int i = Math.min(parts.length, 3); i >= 1; i--) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < i; j++) {
                if (j > 0) sb.append(" ");
                sb.append(parts[j]);
            }
            String partial = sb.toString();
            if (addressToCodeMap.containsKey(partial)) {
                return Optional.of(addressToCodeMap.get(partial));
            }
        }

        return Optional.empty();
    }
}
