package com.example.budongbatch.domain.realdeal.entity;

import com.example.budongbatch.common.entity.BaseEntity;
import com.example.budongbatch.common.enums.GeoStatus;
import com.example.budongbatch.common.enums.PropertyType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 실거래가 엔티티
 *
 * 국토교통부 공공데이터에서 수집한 아파트/오피스텔/빌라 실거래 정보
 * - 지오코딩 전: latitude/longitude = null
 * - 지오코딩 후: 좌표 + 도로명 주소 저장
 * - 지오코딩 실패: (0, 0) 저장하여 재처리 방지
 */
@Getter
@Entity
@Table(name = "real_deals")
// 인덱스/유니크 제약은 resources/db/v2_real_deal_index.sql 에서 관리
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RealDeal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_name", length = 100, nullable = false)
    private String propertyName;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "deal_amount", nullable = false)
    private BigDecimal dealAmount;

    @Column(name = "exclusive_area", precision = 10, scale = 2)
    private BigDecimal exclusiveArea;

    @Column(name = "floor")
    private Integer floor;

    @Column(name = "built_year")
    private Integer builtYear;

    @Column(name = "deal_date", nullable = false)
    private LocalDate dealDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", length = 20, nullable = false)
    private PropertyType propertyType;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "road_address", length = 255)
    private String roadAddress;

    @Column(name = "lawd_cd", length = 5)
    private String lawdCd;

    @Enumerated(EnumType.STRING)
    @Column(name = "geo_status", length = 10, nullable = false)
    private GeoStatus geoStatus = GeoStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Builder
    public RealDeal(String propertyName, String address, BigDecimal dealAmount,
                    BigDecimal exclusiveArea, Integer floor, Integer builtYear,
                    LocalDate dealDate, PropertyType propertyType, String lawdCd) {
        this.propertyName = propertyName;
        this.address = address;
        this.dealAmount = dealAmount;
        this.exclusiveArea = exclusiveArea;
        this.floor = floor;
        this.builtYear = builtYear;
        this.dealDate = dealDate;
        this.propertyType = propertyType;
        this.lawdCd = lawdCd;
    }

    /**
     * 지오코딩 성공 시 좌표 적용
     */
    public void applyGeoCode(BigDecimal latitude, BigDecimal longitude, String roadAddress) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.roadAddress = roadAddress;
        this.geoStatus = GeoStatus.SUCCESS;
    }

    /**
     * 지오코딩 실패 처리
     * @param permanent true: 영구 실패(FAILED), false: 재시도 대상(RETRY)
     */
    public void markGeoFailed(boolean permanent) {
        if (permanent) {
            this.geoStatus = GeoStatus.FAILED;
        } else {
            this.geoStatus = GeoStatus.RETRY;
            this.retryCount++;
        }
    }

    /**
     * 재시도 횟수 초과 시 영구 실패로 전환
     */
    public void markExhausted() {
        this.geoStatus = GeoStatus.FAILED;
    }

    public boolean isGeoCoded() {
        return this.geoStatus == GeoStatus.SUCCESS;
    }

    public boolean needsRetry(int maxRetry) {
        return this.geoStatus == GeoStatus.RETRY && this.retryCount < maxRetry;
    }
}
