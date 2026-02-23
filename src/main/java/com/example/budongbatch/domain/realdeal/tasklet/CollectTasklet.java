package com.example.budongbatch.domain.realdeal.tasklet;

import com.example.budongbatch.common.enums.PropertyType;
import com.example.budongbatch.domain.realdeal.client.publicdata.AptClient;
import com.example.budongbatch.domain.realdeal.client.publicdata.AptItem;
import com.example.budongbatch.domain.realdeal.client.publicdata.AptResponse;
import com.example.budongbatch.domain.realdeal.client.publicdata.OffiClient;
import com.example.budongbatch.domain.realdeal.client.publicdata.VillaClient;
import com.example.budongbatch.domain.realdeal.converter.RealDealConverter;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.domain.realdeal.service.CollectHistoryService;
import com.example.budongbatch.domain.realdeal.service.LawdCodeService;
import com.example.budongbatch.domain.realdeal.service.RealDealSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 실거래가 데이터 수집 Tasklet
 *
 * 전국 법정동 코드 순회
 * 공공데이터 API 호출 (아파트/오피스텔/빌라)
 * 수집 데이터 DB 저장 (중복 무시)
 * 변환 로직 분리: RealDealConverter로 분리하여 테스트 용이성 확보
 */
@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class CollectTasklet implements Tasklet {

    private final LawdCodeService lawdCodeService;
    private final AptClient aptClient;
    private final OffiClient offiClient;
    private final VillaClient villaClient;
    private final RealDealSaveService realDealSaveService;
    private final RealDealConverter realDealConverter;
    private final CollectHistoryService collectHistoryService;

    @Value("${external.api.service-key}")
    private String serviceKey;

    @Value("#{jobParameters['runDate']}")
    private String runDate;

    @Value("#{jobParameters['dealYmd'] ?: ''}")
    private String dealYmdParam;

    private static final int NUM_OF_ROWS = 1000;
    private static final DateTimeFormatter DEAL_YMD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        String dealYmd = resolveDealYmd();
        CollectHistoryService.CollectInitResult init = collectHistoryService.init(dealYmd);
        if (!init.shouldRun()) {
            log.info("{}월 데이터 이미 수집 완료 - 스킵", dealYmd);
            return RepeatStatus.FINISHED;
        }

        List<String> allLawdCodes = lawdCodeService.getAllLawdCodes();
        List<String> lawdCodes = collectHistoryService.resolveTargetLawdCodes(dealYmd, allLawdCodes);

        log.info("[수집 시작] 대상월: {} | 법정동: {}개", dealYmd, lawdCodes.size());

        int totalCollected = 0;
        int totalSaved = 0;
        int processedCount = 0;
        List<String> failedLawdCodes = new ArrayList<>();

        for (String lawdCd : lawdCodes) {
            try {
                CollectResult result = collectByLawdCd(lawdCd, dealYmd);
                totalCollected += result.collected;
                totalSaved += result.saved;
                processedCount++;

                // 100개 법정동마다 진행상황 로그
                if (processedCount % 100 == 0) {
                    log.info("[진행] {}/{} 법정동 처리 | 수집: {}건 | 저장: {}건",
                            processedCount, lawdCodes.size(), totalCollected, totalSaved);
                }
            } catch (Exception e) {
                log.warn("법정동 {} 수집 실패: {}", lawdCd, e.getMessage());
                failedLawdCodes.add(lawdCd);
            }
        }

        int duplicates = totalCollected - totalSaved;
        collectHistoryService.finish(dealYmd, totalCollected, failedLawdCodes);
        log.info("[수집 완료] 법정동: {}개 | API 수집: {}건 | 저장: {}건 | 중복 스킵: {}건",
                processedCount, totalCollected, totalSaved, duplicates);

        return RepeatStatus.FINISHED;
    }

    /** 수집 결과 (수집 건수 + 저장 건수) */
    private record CollectResult(int collected, int saved) {}

    /**
     * 수집 대상 월 결정
     * 우선순위: dealYmd > runDate > 전월
     *
     * dealYmd: 직접 지정 (예: 202512)
     * runDate: 날짜에서 추출 (예: 2025-12-01 → 202512)
     */
    private String resolveDealYmd() {
        // 1. dealYmd 직접 지정 (예: 202512)
        if (dealYmdParam != null && !dealYmdParam.isBlank()) {
            log.info("dealYmd 파라미터 사용: {}", dealYmdParam);
            return dealYmdParam;
        }

        // 2. runDate에서 추출 (예: 2025-12-01 → 202512)
        if (runDate != null && !runDate.isBlank()) {
            try {
                String ym = YearMonth.from(java.time.LocalDate.parse(runDate)).format(DEAL_YMD_FORMAT);
                log.info("runDate에서 추출: {} → {}", runDate, ym);
                return ym;
            } catch (Exception e) {
                log.warn("runDate 파싱 실패, 전월 기준으로 수집합니다: {}", runDate);
            }
        }

        // 3. 기본값: 전월
        return YearMonth.now().minusMonths(1).format(DEAL_YMD_FORMAT);
    }

    private CollectResult collectByLawdCd(String lawdCd, String dealYmd) {
        List<RealDeal> allDeals = new ArrayList<>();
        String addressPrefix = lawdCodeService.getAddressPrefixFromLawdCd(lawdCd).orElse("");

        allDeals.addAll(collectApt(lawdCd, dealYmd, addressPrefix));
        allDeals.addAll(collectOffi(lawdCd, dealYmd, addressPrefix));
        allDeals.addAll(collectVilla(lawdCd, dealYmd, addressPrefix));

        int saved = saveDeals(allDeals);

        if (!allDeals.isEmpty()) {
            log.debug("법정동 {} - 수집: {}건, 저장: {}건", lawdCd, allDeals.size(), saved);
        }

        return new CollectResult(allDeals.size(), saved);
    }

    private List<RealDeal> collectApt(String lawdCd, String dealYmd, String addressPrefix) {
        return collectFromApi(lawdCd, dealYmd, addressPrefix, PropertyType.APARTMENT,
                (lc, ym, page) -> aptClient.getApt(serviceKey, lc, ym, page, NUM_OF_ROWS));
    }

    private List<RealDeal> collectOffi(String lawdCd, String dealYmd, String addressPrefix) {
        return collectFromApi(lawdCd, dealYmd, addressPrefix, PropertyType.OFFICETEL,
                (lc, ym, page) -> offiClient.getOffi(serviceKey, lc, ym, page, NUM_OF_ROWS));
    }

    private List<RealDeal> collectVilla(String lawdCd, String dealYmd, String addressPrefix) {
        return collectFromApi(lawdCd, dealYmd, addressPrefix, PropertyType.VILLA,
                (lc, ym, page) -> villaClient.getVilla(serviceKey, lc, ym, page, NUM_OF_ROWS));
    }

    // 공공데이터 API 페이징 수집 로직
    // 네트워크 오류/5xx/429 등 일시적 장애는 Feign Retry 설정을 통해 재시도한다.
    private List<RealDeal> collectFromApi(String lawdCd, String dealYmd, String addressPrefix,
                                          PropertyType propertyType, ApiCaller apiCaller) {
        List<RealDeal> deals = new ArrayList<>();
        int pageNo = 1;

        while (true) {
            try {
                AptResponse response = apiCaller.call(lawdCd, dealYmd, pageNo);
                List<AptItem> items = extractItems(response);

                if (items.isEmpty()) break;

                for (AptItem item : items) {
                    RealDeal deal = realDealConverter.convert(item, lawdCd, propertyType, addressPrefix);
                    if (deal != null) {
                        deals.add(deal);
                    }
                }

                if (items.size() < NUM_OF_ROWS) break;
                pageNo++;
            } catch (Exception e) {
                log.warn("{} API 호출 실패 - lawdCd: {}, page: {}, error: {}",
                        propertyType, lawdCd, pageNo, e.getMessage());
                break;
            }
        }
        return deals;
    }

    // Apt/Offi/Villa 3개 클라이언트 호출 로직이 동일한데, 클라이언트만 다르게 넘기려고 만듦
    @FunctionalInterface
    interface ApiCaller {
        AptResponse call(String lawdCd, String dealYmd, int pageNo);
    }

    private List<AptItem> extractItems(AptResponse response) {
        if (response == null || response.response() == null
                || response.response().body() == null
                || response.response().body().items() == null
                || response.response().body().items().item() == null) {
            return List.of();
        }
        return response.response().body().items().item();
    }

    private int saveDeals(List<RealDeal> deals) {
        return realDealSaveService.saveAllIgnoreDuplicates(deals);
    }
}
