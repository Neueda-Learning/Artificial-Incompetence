package com.hsbc.portfoliomanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PortfolioManagerApplication {

    /**
     * 中文：应用程序入口，启动 Spring Boot、组件扫描和内嵌 Web 服务器。
     * English: Application entry point that starts Spring Boot, component scanning, and the embedded web server.
     */
    public static void main(String[] args) {
        SpringApplication.run(PortfolioManagerApplication.class, args);
    }
}
