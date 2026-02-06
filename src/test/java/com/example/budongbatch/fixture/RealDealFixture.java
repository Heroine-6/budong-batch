package com.example.budongbatch.fixture;

import com.example.budongbatch.common.enums.GeoStatus;
import com.example.budongbatch.common.enums.PropertyType;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * RealDeal 테스트 Fixture
 */
public class RealDealFixture {

    public static RealDeal create() {
        return RealDeal.builder()
                .propertyName("래미안아파트")
                .address("서울특별시 강남구 역삼동 123-45")
                .dealAmount(new BigDecimal("12500"))
                .exclusiveArea(new BigDecimal("84.99"))
                .floor(10)
                .builtYear(2020)
                .dealDate(LocalDate.of(2025, 1, 15))
                .propertyType(PropertyType.APARTMENT)
                .lawdCd("11680")
                .build();
    }

    /**
     * 지오코딩 대기 상태 (PENDING)
     */
    public static RealDeal pending() {
        return create();  // 기본 생성 시 PENDING
    }

    /**
     * 재시도 상태 (RETRY)
     */
    public static RealDeal retry(int retryCount) {
        RealDeal deal = create();
        setField(deal, "geoStatus", GeoStatus.RETRY);
        setField(deal, "retryCount", retryCount);
        return deal;
    }

    /**
     * 지오코딩 완료 상태 (SUCCESS)
     */
    public static RealDeal success() {
        return successWithId(1L);
    }

    /**
     * 지오코딩 완료 상태 (SUCCESS) - ID 지정
     */
    public static RealDeal successWithId(Long id) {
        RealDeal deal = create();
        deal.applyGeoCode(
                new BigDecimal("37.5012743"),
                new BigDecimal("127.0396597"),
                "서울특별시 강남구 역삼로 123"
        );
        setField(deal, "id", id);
        return deal;
    }

    /**
     * Reflection으로 필드 값 변경
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
