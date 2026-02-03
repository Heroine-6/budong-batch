package com.example.budongbatch.domain.realdeal.client.publicdata;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "villaClient",
        url = "https://apis.data.go.kr/1613000/RTMSDataSvcRHTrade"
)
public interface VillaClient {

    @GetMapping(
            value = "/getRTMSDataSvcRHTrade?resultType=json",
            produces = "application/json"
    )
    AptResponse getVilla(
            @RequestParam("serviceKey") String serviceKey,
            @RequestParam("LAWD_CD") String lawdCd,
            @RequestParam("DEAL_YMD") String dealYmd,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "numOfRows", defaultValue = "10") int numOfRows
    );
}
