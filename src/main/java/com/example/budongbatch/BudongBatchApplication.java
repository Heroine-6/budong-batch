package com.example.budongbatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BudongBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BudongBatchApplication.class, args);
    }

}
