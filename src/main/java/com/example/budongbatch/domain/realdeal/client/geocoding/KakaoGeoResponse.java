package com.example.budongbatch.domain.realdeal.client.geocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoGeoResponse(
        List<Document> documents
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            @JsonProperty("address_name") String addressName,
            String x,
            String y
    ) {}

    public boolean hasResult() {
        return documents != null && !documents.isEmpty();
    }
}
