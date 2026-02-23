package com.example.budongbatch.domain.realdeal.repository;

import com.example.budongbatch.common.enums.GeoStatus;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RealDealRepository extends JpaRepository<RealDeal, Long> {

    // PENDING 상태 조회 (신규 지오코딩 대상)
    List<RealDeal> findByGeoStatus(GeoStatus geoStatus, Pageable pageable);

    // RETRY 상태 중 재시도 횟수 미만인 건 조회
    List<RealDeal> findByGeoStatusAndRetryCountLessThan(GeoStatus geoStatus, int maxRetry, Pageable pageable);

    long countByGeoStatus(GeoStatus geoStatus);

    long countByGeoStatusAndRetryCountLessThan(GeoStatus geoStatus, int maxRetry);

    @Query("""
            SELECT rd FROM RealDeal rd
            WHERE rd.id > :lastId
              AND rd.geoStatus = 'SUCCESS'
              AND rd.isDeleted = false
            ORDER BY rd.id ASC
            """)
    List<RealDeal> findGeoCodedAfter(@Param("lastId") Long lastId, Pageable pageable);

    // 파티셔닝용: PENDING/RETRY 상태의 최소 ID
    @Query("""
            SELECT MIN(rd.id) FROM RealDeal rd
            WHERE rd.geoStatus IN ('PENDING', 'RETRY')
              AND rd.isDeleted = false
            """)
    Long findMinIdByGeoStatusIn();

    // 파티셔닝용: PENDING/RETRY 상태의 최대 ID
    @Query("""
            SELECT MAX(rd.id) FROM RealDeal rd
            WHERE rd.geoStatus IN ('PENDING', 'RETRY')
              AND rd.isDeleted = false
            """)
    Long findMaxIdByGeoStatusIn();

    // 파티션별 ID 범위 조회
    @Query("""
            SELECT rd FROM RealDeal rd
            WHERE rd.id BETWEEN :minId AND :maxId
              AND rd.geoStatus IN ('PENDING', 'RETRY')
              AND (rd.geoStatus = 'PENDING' OR rd.retryCount < :maxRetry)
              AND rd.isDeleted = false
            ORDER BY rd.id ASC
            """)
    List<RealDeal> findByIdRangeAndGeoStatus(
            @Param("minId") Long minId,
            @Param("maxId") Long maxId,
            @Param("maxRetry") int maxRetry,
            Pageable pageable);

    // FAILED → RETRY 상태 변경 (재시도 대상으로 전환)
    @Modifying
    @Query("""
            UPDATE RealDeal rd
            SET rd.geoStatus = 'RETRY', rd.retryCount = 0
            WHERE rd.geoStatus = 'FAILED'
              AND rd.isDeleted = false
            """)
    int resetFailedToRetry();
}
