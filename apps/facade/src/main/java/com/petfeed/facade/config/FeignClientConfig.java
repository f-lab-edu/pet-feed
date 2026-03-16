package com.petfeed.facade.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.petfeed.facade.client")
public class FeignClientConfig {
}
