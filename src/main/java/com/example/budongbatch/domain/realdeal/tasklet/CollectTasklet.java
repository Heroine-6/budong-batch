package com.example.budongbatch.domain.realdeal.tasklet;

import com.example.budongbatch.common.enums.PropertyType;
import com.example.budongbatch.domain.realdeal.client.publicdata.AptClient;
import com.example.budongbatch.domain.realdeal.client.publicdata.AptItem;
import com.example.budongbatch.domain.realdeal.client.publicdata.AptResponse;
import com.example.budongbatch.domain.realdeal.client.publicdata.OffiClient;
import com.example.budongbatch.domain.realdeal.client.publicdata.VillaClient;
import com.example.budongbatch.domain.realdeal.converter.RealDealConverter;
import com.example.budongbatch.domain.realdeal.entity.RealDeal;
import com.example.budongbatch.domain.realdeal.repository.RealDealRepository;
import com.example.budongbatch.domain.realdeal.service.LawdCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
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
@Component
@RequiredArgsConstructor
public class CollectTasklet implements Tasklet {

    private final LawdCodeService lawdCodeService;
    private final AptClient aptClient;
    private final OffiClient offiClient;
    private final VillaClient villaClient;
    private final RealDealRepository realDealRepository;
    private final RealDealConverter realDealConverter;

    @Value("${external.api.service-key}")
    private String serviceKey;

    private static final int NUM_OF_ROWS = 1000;
    private static final DateTimeFormatter DEAL_YMD_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        String dealYmd = YearMonth.now().format(DEAL_YMD_FORMAT);
        List<String> lawdCodes = lawdCodeService.getAllLawdCodes();

        int totalCollected = 0;

        for (String lawdCd : lawdCodes) {
            try {
                int collected = collectByLawdCd(lawdCd, dealYmd);
                totalCollected += collected;
            } catch (Exception e) {
                log.warn("법정동 {} 수집 실패: {}", lawdCd, e.getMessage());
            }
        }

        log.info("실거래가 수집 완료 - 총 수집: {}건", totalCollected);
        return RepeatStatus.FINISHED;
    }

    private int collectByLawdCd(String lawdCd, String dealYmd) {
        List<RealDeal> allDeals = new ArrayList<>();
        String addressPrefix = lawdCodeService.getAddressPrefixFromLawdCd(lawdCd).orElse("");

        allDeals.addAll(collectApt(lawdCd, dealYmd, addressPrefix));
        allDeals.addAll(collectOffi(lawdCd, dealYmd, addressPrefix));
        allDeals.addAll(collectVilla(lawdCd, dealYmd, addressPrefix));

        int saved = saveDeals(allDeals);

        if (!allDeals.isEmpty()) {
            log.debug("법정동 {} 수집 완료 - 수집: {}건, 저장: {}건", lawdCd, allDeals.size(), saved);
        }

        return allDeals.size();
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
        int saved = 0;
        for (RealDeal deal : deals) {
            try {
                realDealRepository.save(deal);
                saved++;
            } catch (DataIntegrityViolationException e) {
                // 중복 데이터 무시 (유니크 제약 위반)
                log.debug("저장 스킵 - 중복 데이터: name={}, dealDate={}, error={}",
                        deal.getPropertyName(), deal.getDealDate(), e.getMessage());
            }
        }
        return saved;
    }
}
