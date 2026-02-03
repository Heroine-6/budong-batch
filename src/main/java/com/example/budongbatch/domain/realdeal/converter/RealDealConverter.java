package com.example.budongbatch.domain.realdeal.converter;

import com.example.budongbatch.common.enums.PropertyType;
import com.example.budongbatch.domain.realdeal.client.publicdata.AptItem;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.domain.realdeal.service.LawdCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * AptItem(API 응답) -> RealDeal(엔티티) 변환기
 *
 * 분리 이유
 * - 단일 책임 원칙(SRP): Tasklet은 배치 흐름 제어만, 변환 로직은 Converter가 담당
 * - 테스트 용이성: 변환 로직을 독립적으로 단위 테스트 가능
 * - 재사용성: 다른 곳에서 동일한 변환 로직 필요 시 재사용 가능
 */
@Component
@RequiredArgsConstructor
public class RealDealConverter {

    private final LawdCodeService lawdCodeService;

    /**
     * API 응답 아이템을 RealDeal 엔티티로 변환
     *
     * @param item API 응답 아이템
     * @param lawdCd 법정동 코드 (5자리)
     * @param propertyType 매물 유형 (APARTMENT, OFFICETEL, VILLA)
     * @return 변환된 RealDeal 엔티티, 필수 데이터 누락 시 null
     */
    public RealDeal convert(AptItem item, String lawdCd, PropertyType propertyType) {
        String name = item.getName();
        LocalDate dealDate = item.getDealDate();

        // 필수 데이터 검증
        if (name == null || dealDate == null) {
            return null;
        }

        BigDecimal dealAmount = parseDealAmount(item.dealAmount());
        if (dealAmount == null) {
            return null;
        }

        String addressPrefix = lawdCodeService.getAddressPrefixFromLawdCd(lawdCd).orElse("");
        String address = buildAddress(addressPrefix, item.umdNm(), item.jibun());

        BigDecimal exclusiveArea = parseExclusiveArea(item.excluUseAr());
        Integer builtYear = item.buildYear() != null ? item.buildYear().getValue() : null;

        return RealDeal.builder()
                .propertyName(name)
                .address(address)
                .dealAmount(dealAmount)
                .exclusiveArea(exclusiveArea)
                .floor(item.floor())
                .builtYear(builtYear)
                .dealDate(dealDate)
                .propertyType(propertyType)
                .lawdCd(lawdCd)
                .build();
    }

    /**
     * 주소 문자열 생성
     *
     * @param prefix 시도 + 시군구 (예: "서울특별시 강남구")
     * @param umdNm 읍면동명 (예: "역삼동")
     * @param jibun 지번 (예: "123-45")
     * @return 조합된 주소 (예: "서울특별시 강남구 역삼동 123-45")
     */
    String buildAddress(String prefix, String umdNm, String jibun) {
        StringBuilder sb = new StringBuilder();

        if (prefix != null && !prefix.isBlank()) {
            sb.append(prefix);
        }
        if (umdNm != null && !umdNm.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(umdNm);
        }
        if (jibun != null && !jibun.isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(jibun);
        }

        return sb.toString();
    }

    /**
     * 거래금액 문자열을 BigDecimal로 파싱
     *
     * <p>공공데이터 API는 금액을 "12,500" 형태의 문자열로 반환.
     * 콤마와 공백을 제거 후 숫자로 변환.</p>
     *
     * @param dealAmount 거래금액 문자열 (예: "12,500", " 8,000 ")
     * @return 파싱된 금액, 파싱 실패 시 null
     */
    BigDecimal parseDealAmount(String dealAmount) {
        if (dealAmount == null || dealAmount.isBlank()) {
            return null;
        }
        try {
            String cleaned = dealAmount.replaceAll("[,\\s]", "");
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 전용면적 문자열을 BigDecimal로 파싱
     *
     * @param area 전용면적 문자열 (예: "84.99")
     * @return 파싱된 면적, 파싱 실패 시 null
     */
    BigDecimal parseExclusiveArea(String area) {
        if (area == null || area.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(area.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
