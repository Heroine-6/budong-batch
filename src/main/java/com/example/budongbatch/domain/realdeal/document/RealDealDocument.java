package com.example.budongbatch.domain.realdeal.document;

import com.example.budongbatch.common.enums.PropertyType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Elasticsearch 실거래가 문서
 *
 * - 위치 기반 검색을 위한 geo_point 필드 포함
 * - nori_analyzer로 한글 주소 검색 지원
 * - 주변 시세 조회, 입찰가 비교 등에 활용
 */
@Getter
@Document(indexName = "real_deal_search")
@Setting(settingPath = "elasticsearch/settings.json")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RealDealDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String propertyName;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String address;

    @Field(type = FieldType.Text, analyzer = "nori_analyzer")
    private String roadAddress;

    @Field(type = FieldType.Long)
    private BigDecimal dealAmount;

    @Field(type = FieldType.Double)
    private BigDecimal exclusiveArea;

    @Field(type = FieldType.Integer)
    private Integer floor;

    @Field(type = FieldType.Integer)
    private Integer builtYear;

    @Field(type = FieldType.Date)
    private LocalDate dealDate;

    @Field(type = FieldType.Keyword)
    private PropertyType propertyType;

    @GeoPointField
    private GeoPoint location;
}
