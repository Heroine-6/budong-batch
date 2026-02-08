package com.example.budongbatch.domain.realdeal.service;

import com.example.budongbatch.domain.realdeal.entity.BatchDealCollectFailedLawd;
import com.example.budongbatch.domain.realdeal.entity.BatchDealCollectHistory;
import com.example.budongbatch.domain.realdeal.enums.CollectStatus;
import com.example.budongbatch.domain.realdeal.repository.BatchDealCollectFailedLawdRepository;
import com.example.budongbatch.domain.realdeal.repository.BatchDealCollectHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectHistoryServiceTest {

    @Mock
    private BatchDealCollectHistoryRepository historyRepository;

    @Mock
    private BatchDealCollectFailedLawdRepository failedLawdRepository;

    @Test
    @DisplayName("이력이 없으면 RUNNING으로 시작한다")
    void init_createsHistoryWhenMissing() {
        CollectHistoryService service = new CollectHistoryService(historyRepository, failedLawdRepository);
        when(historyRepository.findById("202512")).thenReturn(Optional.empty());

        CollectHistoryService.CollectInitResult result = service.init("202512");

        assertThat(result.shouldRun()).isTrue();
        assertThat(result.resumed()).isFalse();

        ArgumentCaptor<BatchDealCollectHistory> captor = ArgumentCaptor.forClass(BatchDealCollectHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CollectStatus.RUNNING);
        assertThat(captor.getValue().getDealYmd()).isEqualTo("202512");
    }

    @Test
    @DisplayName("SUCCESS 이력은 스킵한다")
    void init_skipsWhenSuccess() {
        CollectHistoryService service = new CollectHistoryService(historyRepository, failedLawdRepository);
        BatchDealCollectHistory history = BatchDealCollectHistory.start("202512", LocalDateTime.now());
        history.finish(CollectStatus.SUCCESS, 10, 0, LocalDateTime.now());
        when(historyRepository.findById("202512")).thenReturn(Optional.of(history));

        CollectHistoryService.CollectInitResult result = service.init("202512");

        assertThat(result.shouldRun()).isFalse();
        assertThat(result.resumed()).isFalse();
    }

    @Test
    @DisplayName("RUNNING이 24시간 이상이면 FAILED로 전환 후 재개한다")
    void init_marksRunningTimeoutToFailed() {
        CollectHistoryService service = new CollectHistoryService(historyRepository, failedLawdRepository);
        BatchDealCollectHistory history = BatchDealCollectHistory.start("202512", LocalDateTime.now().minusDays(2));
        when(historyRepository.findById("202512")).thenReturn(Optional.of(history));

        CollectHistoryService.CollectInitResult result = service.init("202512");

        assertThat(result.shouldRun()).isTrue();
        assertThat(result.resumed()).isTrue();
        ArgumentCaptor<BatchDealCollectHistory> captor = ArgumentCaptor.forClass(BatchDealCollectHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CollectStatus.RUNNING);
    }

    @Test
    @DisplayName("실패 법정동이 있으면 해당 코드만 반환하고 기록을 삭제한다")
    void resolveTargetLawdCodes_usesFailedList() {
        CollectHistoryService service = new CollectHistoryService(historyRepository, failedLawdRepository);
        when(failedLawdRepository.findByDealYmd("202512"))
                .thenReturn(List.of(
                        BatchDealCollectFailedLawd.of("202512", "11110"),
                        BatchDealCollectFailedLawd.of("202512", "11140")
                ));

        List<String> targets = service.resolveTargetLawdCodes("202512", List.of("11110", "11140", "11170"));

        assertThat(targets).containsExactlyInAnyOrder("11110", "11140");
        verify(failedLawdRepository).deleteByDealYmd("202512");
    }

    @Test
    @DisplayName("실패 법정동이 없으면 전체 코드를 반환한다")
    void resolveTargetLawdCodes_returnsAllWhenNoFailed() {
        CollectHistoryService service = new CollectHistoryService(historyRepository, failedLawdRepository);
        when(failedLawdRepository.findByDealYmd("202512")).thenReturn(List.of());

        List<String> targets = service.resolveTargetLawdCodes("202512", List.of("11110", "11140"));

        assertThat(targets).containsExactly("11110", "11140");
    }

    @Test
    @DisplayName("실패가 없으면 SUCCESS로 저장한다")
    void finish_marksSuccess() {
        CollectHistoryService service = new CollectHistoryService(historyRepository, failedLawdRepository);
        when(historyRepository.findById("202512")).thenReturn(Optional.of(
                BatchDealCollectHistory.start("202512", LocalDateTime.now())
        ));

        service.finish("202512", 100, List.of());

        ArgumentCaptor<BatchDealCollectHistory> captor = ArgumentCaptor.forClass(BatchDealCollectHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CollectStatus.SUCCESS);
        assertThat(captor.getValue().getCollectedCount()).isEqualTo(100);
        assertThat(captor.getValue().getFailedLawdCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("실패가 있으면 FAILED로 저장하고 실패 법정동을 기록한다")
    void finish_marksFailedAndStoresFailedLawd() {
        CollectHistoryService service = new CollectHistoryService(historyRepository, failedLawdRepository);
        when(historyRepository.findById("202512")).thenReturn(Optional.of(
                BatchDealCollectHistory.start("202512", LocalDateTime.now())
        ));

        service.finish("202512", 100, List.of("11110", "11140"));

        ArgumentCaptor<BatchDealCollectHistory> captor = ArgumentCaptor.forClass(BatchDealCollectHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(CollectStatus.FAILED);
        assertThat(captor.getValue().getFailedLawdCount()).isEqualTo(2);

        verify(failedLawdRepository).saveAll(any());
    }
}
