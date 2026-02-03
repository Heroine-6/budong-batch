package com.example.budongbatch.domain.realdeal.client.publicdata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AptResponse(
        Response response
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            Body body
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Body(
                Items items
        ) {
            @JsonIgnoreProperties(ignoreUnknown = true)
            public record Items(
                    List<AptItem> item
            ) { }
        }
    }
}
