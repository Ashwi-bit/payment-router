package com.ashwini.router;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PaymentRouterApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentRouterApplication.class, args);
    }
}