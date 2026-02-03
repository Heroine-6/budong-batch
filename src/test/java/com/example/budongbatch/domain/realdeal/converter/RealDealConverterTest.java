package com.example.budongbatch.domain.realdeal.converter;

import com.example.budongbatch.common.enums.PropertyType;
import com.example.budongbatch.domain.realdeal.client.publicdata.AptItem;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.fixture.AptItemFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RealDealConverter 단위 테스트
 * - 외부 의존성 없이 순수 변환 로직만 테스트
 * - AptItem → RealDeal 변환 성공/실패 케이스
 * - 거래금액 파싱 (콤마, 공백 처리)
 * - 주소 조합 로직
 * - 전용면적 파싱
 */
class RealDealConverterTest {

    private static final String LAWD_CD = "11680";
    private static final String ADDRESS_PREFIX = "서울특별시 강남구";

    private RealDealConverter converter;

    @BeforeEach
    void setUp() {
        converter = new RealDealConverter();
    }

    @Nested
    @DisplayName("convert() - AptItem을 RealDeal로 변환")
    class ConvertTest {

        @Test
        @DisplayName("정상 데이터 변환 성공")
        void convert_success() {
            // given
            AptItem item = AptItemFixture.create();

            // when
            RealDeal result = converter.convert(item, LAWD_CD, PropertyType.APARTMENT, ADDRESS_PREFIX);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getPropertyName()).isEqualTo("래미안아파트");
            assertThat(result.getAddress()).isEqualTo("서울특별시 강남구 역삼동 123-45");
            assertThat(result.getDealAmount()).isEqualByComparingTo(new BigDecimal("12500"));
            assertThat(result.getExclusiveArea()).isEqualByComparingTo(new BigDecimal("84.99"));
            assertThat(result.getFloor()).isEqualTo(10);
            assertThat(result.getBuiltYear()).isEqualTo(2020);
            assertThat(result.getPropertyType()).isEqualTo(PropertyType.APARTMENT);
            assertThat(result.getLawdCd()).isEqualTo(LAWD_CD);
        }

        @Test
        @DisplayName("이름이 null이면 null 반환")
        void convert_nullName_returnsNull() {
            // given
            AptItem item = AptItemFixture.withNullName();

            // when
            RealDeal result = converter.convert(item, LAWD_CD, PropertyType.APARTMENT, ADDRESS_PREFIX);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("거래일자가 null이면 null 반환")
        void convert_nullDealDate_returnsNull() {
            // given
            AptItem item = AptItemFixture.withNullDealDate();

            // when
            RealDeal result = converter.convert(item, LAWD_CD, PropertyType.APARTMENT, ADDRESS_PREFIX);

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("거래금액 파싱 실패 시 null 반환")
        void convert_invalidDealAmount_returnsNull() {
            // given
            AptItem item = AptItemFixture.withInvalidDealAmount();

            // when
            RealDeal result = converter.convert(item, LAWD_CD, PropertyType.APARTMENT, ADDRESS_PREFIX);

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("parseDealAmount() - 거래금액 파싱")
    class ParseDealAmountTest {

        @Test
        @DisplayName("콤마가 포함된 금액 파싱")
        void parse_withComma() {
            BigDecimal result = converter.parseDealAmount("12,500");
            assertThat(result).isEqualByComparingTo(new BigDecimal("12500"));
        }

        @Test
        @DisplayName("공백이 포함된 금액 파싱")
        void parse_withSpaces() {
            BigDecimal result = converter.parseDealAmount(" 8,000 ");
            assertThat(result).isEqualByComparingTo(new BigDecimal("8000"));
        }

        @Test
        @DisplayName("빈 문자열 입력 시 null 반환")
        void parse_empty_returnsNull() {
            assertThat(converter.parseDealAmount("")).isNull();
            assertThat(converter.parseDealAmount("   ")).isNull();
        }

        @Test
        @DisplayName("잘못된 형식 입력 시 null 반환")
        void parse_invalid_returnsNull() {
            assertThat(converter.parseDealAmount("abc")).isNull();
            assertThat(converter.parseDealAmount("12.34.56")).isNull();
        }
    }

    @Nested
    @DisplayName("buildAddress() - 주소 조합")
    class BuildAddressTest {

        @Test
        @DisplayName("모든 필드가 있을 때 전체 주소 생성")
        void build_allFields() {
            String result = converter.buildAddress("서울특별시 강남구", "역삼동", "123-45");
            assertThat(result).isEqualTo("서울특별시 강남구 역삼동 123-45");
        }

        @Test
        @DisplayName("prefix만 있을 때")
        void build_prefixOnly() {
            String result = converter.buildAddress("서울특별시 강남구", null, null);
            assertThat(result).isEqualTo("서울특별시 강남구");
        }

        @Test
        @DisplayName("prefix 없이 읍면동과 지번만")
        void build_withoutPrefix() {
            String result = converter.buildAddress("", "역삼동", "123-45");
            assertThat(result).isEqualTo("역삼동 123-45");
        }

        @Test
        @DisplayName("모든 필드가 null/빈값이면 빈 문자열")
        void build_allEmpty() {
            String result = converter.buildAddress(null, "", "   ");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("parseExclusiveArea() - 전용면적 파싱")
    class ParseExclusiveAreaTest {

        @Test
        @DisplayName("정상 면적 파싱")
        void parse_normal() {
            BigDecimal result = converter.parseExclusiveArea("84.99");
            assertThat(result).isEqualByComparingTo(new BigDecimal("84.99"));
        }

        @Test
        @DisplayName("공백 포함된 면적 파싱")
        void parse_withSpaces() {
            BigDecimal result = converter.parseExclusiveArea(" 59.5 ");
            assertThat(result).isEqualByComparingTo(new BigDecimal("59.5"));
        }

        @Test
        @DisplayName("null 입력 시 null 반환")
        void parse_null() {
            assertThat(converter.parseExclusiveArea(null)).isNull();
        }
    }
}
