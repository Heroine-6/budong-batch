package com.example.budongbatch.domain.realdeal.reader;

import com.example.budongbatch.common.config.BatchProperties;
import com.example.budongbatch.common.enums.GeoStatus;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.domain.realdeal.repository.RealDealRepository;
import com.example.budongbatch.fixture.RealDealFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingDealReaderTest {

    @Mock
    private RealDealRepository realDealRepository;

    private PendingDealReader reader;

    @BeforeEach
    void setUp() {
        BatchProperties props = new BatchProperties();
        BatchProperties.Geocode geocode = new BatchProperties.Geocode();
        geocode.setPageSize(100);
        geocode.setMaxRetry(3);
        props.setGeocode(geocode);
        reader = new PendingDealReader(realDealRepository, props);
    }

    @Test
    @DisplayName("PENDING 우선 조회 후 소진되면 RETRY로 전환한다")
    void read_pendingThenRetry() {
        RealDeal p1 = RealDealFixture.pending();
        RealDeal p2 = RealDealFixture.pending();
        RealDeal r1 = RealDealFixture.retry(1);

        when(realDealRepository.findByGeoStatus(any(), any(PageRequest.class)))
                .thenReturn(List.of(p1, p2))
                .thenReturn(List.of());
        when(realDealRepository.findByGeoStatusAndRetryCountLessThan(any(), anyInt(), any(PageRequest.class)))
                .thenReturn(List.of(r1))
                .thenReturn(List.of());

        assertThat(reader.read()).isSameAs(p1);
        assertThat(reader.read()).isSameAs(p2);
        assertThat(reader.read()).isSameAs(r1);
        assertThat(reader.read()).isNull();

        verify(realDealRepository, atLeast(1)).findByGeoStatus(eq(GeoStatus.PENDING), any(PageRequest.class));
        verify(realDealRepository, atLeast(1)).findByGeoStatusAndRetryCountLessThan(eq(GeoStatus.RETRY), eq(3), any(PageRequest.class));
    }

    @Test
    @DisplayName("PENDING이 비어 있으면 RETRY만 조회한다")
    void read_pendingEmpty_usesRetryOnly() {
        RealDeal r1 = RealDealFixture.retry(0);

        when(realDealRepository.findByGeoStatus(any(), any(PageRequest.class)))
                .thenReturn(List.of());
        when(realDealRepository.findByGeoStatusAndRetryCountLessThan(any(), anyInt(), any(PageRequest.class)))
                .thenReturn(List.of(r1))
                .thenReturn(List.of());

        assertThat(reader.read()).isSameAs(r1);
        assertThat(reader.read()).isNull();

        verify(realDealRepository, atLeast(1)).findByGeoStatus(eq(GeoStatus.PENDING), any(PageRequest.class));
        verify(realDealRepository, atLeast(1)).findByGeoStatusAndRetryCountLessThan(eq(GeoStatus.RETRY), eq(3), any(PageRequest.class));
    }

    @Test
    @DisplayName("조회는 id ASC 정렬로 수행한다")
    void fetch_usesIdAscSort() {
        when(realDealRepository.findByGeoStatus(any(), any(PageRequest.class)))
                .thenReturn(List.of());
        when(realDealRepository.findByGeoStatusAndRetryCountLessThan(any(), anyInt(), any(PageRequest.class)))
                .thenReturn(List.of());

        reader.read();

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(realDealRepository).findByGeoStatus(eq(GeoStatus.PENDING), captor.capture());

        PageRequest pageRequest = captor.getValue();
        Sort.Order order = pageRequest.getSort().getOrderFor("id");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }
}
