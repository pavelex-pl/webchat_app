package com.webchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WebchatApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebchatApplication.class, args);
    }
}
