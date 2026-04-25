package com.grid.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringGridApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringGridApplication.class, args);
    }
}
