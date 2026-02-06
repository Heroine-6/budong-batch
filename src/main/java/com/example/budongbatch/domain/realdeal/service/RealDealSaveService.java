package com.example.budongbatch.domain.realdeal.service;

import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 실거래가 저장 서비스
 *
 * INSERT IGNORE: 중복 시 무시, 벌크 처리로 성능 향상
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealDealSaveService {

    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 500;

    /**
     * 벌크 저장 (INSERT IGNORE)
     * 중복은 무시하고 신규만 저장
     *
     * @return 실제 저장된 건수
     */
    public int saveAllIgnoreDuplicates(List<RealDeal> deals) {
        if (deals.isEmpty()) {
            return 0;
        }

        String sql = """
            INSERT IGNORE INTO real_deals
            (property_name, address, road_address, deal_amount, deal_date, floor,
             exclusive_area, built_year, property_type, lawd_cd,
             geo_status, retry_count, latitude, longitude, created_at, is_deleted)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        LocalDateTime now = LocalDateTime.now();
        int totalSaved = 0;

        // 배치 단위로 처리
        for (int i = 0; i < deals.size(); i += BATCH_SIZE) {
            List<RealDeal> batch = deals.subList(i, Math.min(i + BATCH_SIZE, deals.size()));

            int[][] results = jdbcTemplate.batchUpdate(sql, batch, BATCH_SIZE, (ps, deal) -> {
                ps.setString(1, deal.getPropertyName());
                ps.setString(2, deal.getAddress());
                ps.setString(3, deal.getRoadAddress());
                ps.setBigDecimal(4, deal.getDealAmount());
                ps.setDate(5, Date.valueOf(deal.getDealDate()));
                ps.setObject(6, deal.getFloor());
                ps.setBigDecimal(7, deal.getExclusiveArea());
                ps.setObject(8, deal.getBuiltYear());
                ps.setString(9, deal.getPropertyType().name());
                ps.setString(10, deal.getLawdCd());
                ps.setString(11, deal.getGeoStatus().name());
                ps.setInt(12, deal.getRetryCount());
                ps.setBigDecimal(13, deal.getLatitude());
                ps.setBigDecimal(14, deal.getLongitude());
                LocalDateTime createdAt = deal.getCreatedAt();
                ps.setTimestamp(15, Timestamp.valueOf(createdAt != null ? createdAt : now));
                ps.setBoolean(16, false);
            });

            for (int[] batchResult : results) {
                for (int result : batchResult) {
                    if (result > 0) totalSaved++;
                }
            }
        }

        int duplicates = deals.size() - totalSaved;
        if (duplicates > 0) {
            log.debug("저장: {}건, 중복 스킵: {}건", totalSaved, duplicates);
        }

        return totalSaved;
    }
}
