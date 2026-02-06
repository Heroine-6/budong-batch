package com.example.budongbatch.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "batch")
public class BatchProperties {

    private Geocode geocode = new Geocode();
    private Index index = new Index();

    @Getter
    @Setter
    public static class Geocode {
        private int chunkSize = 4000;
        private int pageSize = 4000;
        private int maxRetry = 3;
    }

    @Getter
    @Setter
    public static class Index {
        private int chunkSize = 2000;
        private int pageSize = 2000;
    }
}
