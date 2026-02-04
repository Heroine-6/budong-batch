package com.example.budongbatch.domain.realdeal.reader;

import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.domain.realdeal.repository.RealDealRepository;
import com.example.budongbatch.fixture.RealDealFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuccessDealReaderTest {

    @Mock
    private RealDealRepository realDealRepository;

    private SuccessDealReader reader;

    @BeforeEach
    void setUp() {
        reader = new SuccessDealReader(realDealRepository);
    }

    @Test
    @DisplayName("SUCCESS 상태 데이터를 순차적으로 읽는다")
    void read_successDeals() {
        RealDeal s1 = RealDealFixture.successWithId(1L);
        RealDeal s2 = RealDealFixture.successWithId(2L);

        when(realDealRepository.findGeoCodedAfter(eq(0L), any(PageRequest.class)))
                .thenReturn(List.of(s1, s2));
        when(realDealRepository.findGeoCodedAfter(eq(2L), any(PageRequest.class)))
                .thenReturn(List.of());

        reader.open(new ExecutionContext());

        assertThat(reader.read()).isSameAs(s1);
        assertThat(reader.read()).isSameAs(s2);
        assertThat(reader.read()).isNull();
    }

    @Test
    @DisplayName("ExecutionContext에서 lastProcessedId를 복원한다")
    void open_restoresLastProcessedId() {
        RealDeal s3 = RealDealFixture.successWithId(3L);

        when(realDealRepository.findGeoCodedAfter(eq(2L), any(PageRequest.class)))
                .thenReturn(List.of(s3));
        when(realDealRepository.findGeoCodedAfter(eq(3L), any(PageRequest.class)))
                .thenReturn(List.of());

        ExecutionContext context = new ExecutionContext();
        context.putLong("indexStep.lastProcessedId", 2L);

        reader.open(context);

        assertThat(reader.read()).isSameAs(s3);
        assertThat(reader.read()).isNull();

        verify(realDealRepository).findGeoCodedAfter(eq(2L), any(PageRequest.class));
    }

    @Test
    @DisplayName("update 호출 시 lastProcessedId를 ExecutionContext에 저장한다")
    void update_savesLastProcessedId() {
        RealDeal s1 = RealDealFixture.successWithId(5L);

        when(realDealRepository.findGeoCodedAfter(eq(0L), any(PageRequest.class)))
                .thenReturn(List.of(s1));

        ExecutionContext context = new ExecutionContext();
        reader.open(context);
        reader.read();
        reader.update(context);

        assertThat(context.getLong("indexStep.lastProcessedId")).isEqualTo(5L);
    }

    @Test
    @DisplayName("데이터가 없으면 null을 반환한다")
    void read_emptyResult_returnsNull() {
        when(realDealRepository.findGeoCodedAfter(any(), any(PageRequest.class)))
                .thenReturn(List.of());

        reader.open(new ExecutionContext());

        assertThat(reader.read()).isNull();
    }
}
