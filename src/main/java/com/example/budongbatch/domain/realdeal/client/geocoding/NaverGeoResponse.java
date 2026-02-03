package com.example.budongbatch.domain.realdeal.client.geocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverGeoResponse(
        String status,
        Meta meta,
        List<Address> addresses
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(int totalCount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
            String roadAddress,
            String jibunAddress,
            String x,
            String y
    ) {}

    public boolean hasResult() {
        return addresses != null && !addresses.isEmpty();
    }
}
