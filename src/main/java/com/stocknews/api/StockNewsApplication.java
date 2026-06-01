package com.stocknews.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StockNewsApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockNewsApplication.class, args);
    }
}
