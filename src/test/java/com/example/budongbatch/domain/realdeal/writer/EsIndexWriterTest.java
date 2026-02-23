package com.example.budongbatch.domain.realdeal.writer;

import com.example.budongbatch.domain.realdeal.document.RealDealDocument;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.fixture.RealDealFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.IndexQuery;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EsIndexWriterTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Captor
    private ArgumentCaptor<List<IndexQuery>> indexQueriesCaptor;

    private EsIndexWriter writer;

    @BeforeEach
    void setUp() {
        writer = new EsIndexWriter(elasticsearchOperations);
    }

    @Test
    @DisplayName("RealDeal을 RealDealDocument로 변환하여 벌크 색인한다")
    void write_bulkIndexes() throws Exception {
        RealDeal deal1 = RealDealFixture.successWithId(1L);
        RealDeal deal2 = RealDealFixture.successWithId(2L);
        Chunk<RealDeal> chunk = new Chunk<>(List.of(deal1, deal2));

        writer.write(chunk);

        verify(elasticsearchOperations).bulkIndex(indexQueriesCaptor.capture(), eq(RealDealDocument.class));

        List<IndexQuery> captured = indexQueriesCaptor.getValue();
        assertThat(captured).hasSize(2);
        assertThat(captured.get(0).getId()).isEqualTo("1");
        assertThat(captured.get(1).getId()).isEqualTo("2");
    }

    @Test
    @DisplayName("GeoPoint가 올바르게 변환된다")
    void write_convertsGeoPoint() throws Exception {
        RealDeal deal = RealDealFixture.successWithId(1L);
        Chunk<RealDeal> chunk = new Chunk<>(List.of(deal));

        writer.write(chunk);

        verify(elasticsearchOperations).bulkIndex(indexQueriesCaptor.capture(), eq(RealDealDocument.class));

        IndexQuery query = indexQueriesCaptor.getValue().get(0);
        RealDealDocument doc = (RealDealDocument) query.getObject();

        assertThat(doc.getLocation()).isNotNull();
        assertThat(doc.getLocation().getLat()).isEqualTo(deal.getLatitude().doubleValue());
    }
}
