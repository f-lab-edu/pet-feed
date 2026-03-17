package com.petfeed.facade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.petfeed")
public class FacadeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FacadeApplication.class, args);
    }
}
