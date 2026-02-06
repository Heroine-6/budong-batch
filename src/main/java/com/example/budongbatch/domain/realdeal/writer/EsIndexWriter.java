package com.example.budongbatch.domain.realdeal.writer;

import com.example.budongbatch.domain.realdeal.document.RealDealDocument;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Elasticsearch 벌크 색인 Writer
 *
 * RealDeal 엔티티를 RealDealDocument로 변환 후 벌크 색인
 * GeoPoint 필드로 위치 기반 검색 지원
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexWriter implements ItemWriter<RealDeal> {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void write(Chunk<? extends RealDeal> chunk) throws Exception {
        List<IndexQuery> indexQueries = chunk.getItems().stream()
                .map(this::toIndexQuery)
                .toList();

        if (!indexQueries.isEmpty()) {
            try {
                elasticsearchOperations.bulkIndex(indexQueries, RealDealDocument.class);
                log.debug("ES 벌크 색인 완료: {} 건", indexQueries.size());
            } catch (Exception e) {
                log.error("ES 색인 실패: {} 건", indexQueries.size(), e);
                throw e;
            }
        }
    }

    private IndexQuery toIndexQuery(RealDeal realDeal) {
        RealDealDocument document = toDocument(realDeal);
        return new IndexQueryBuilder()
                .withId(String.valueOf(realDeal.getId()))
                .withObject(document)
                .build();
    }

    private RealDealDocument toDocument(RealDeal realDeal) {
        GeoPoint location = null;
        if (realDeal.getLatitude() != null && realDeal.getLongitude() != null) {
            location = new GeoPoint(
                    realDeal.getLatitude().doubleValue(),
                    realDeal.getLongitude().doubleValue()
            );
        }

        return RealDealDocument.builder()
                .id(realDeal.getId())
                .propertyName(realDeal.getPropertyName())
                .address(realDeal.getAddress())
                .roadAddress(realDeal.getRoadAddress())
                .dealAmount(realDeal.getDealAmount())
                .exclusiveArea(realDeal.getExclusiveArea())
                .floor(realDeal.getFloor())
                .builtYear(realDeal.getBuiltYear())
                .dealDate(realDeal.getDealDate())
                .propertyType(realDeal.getPropertyType())
                .location(location)
                .build();
    }
}
