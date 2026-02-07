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
        private int chunkSize;
        private int pageSize;
        private int maxRetry;
        private int partitionSize;
    }

    @Getter
    @Setter
    public static class Index {
        private int chunkSize;
        private int pageSize;
    }
}
