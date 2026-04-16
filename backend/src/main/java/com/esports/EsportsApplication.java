package com.esports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// E-sports 팬 사이트 메인 애플리케이션 진입점
@SpringBootApplication
@ConfigurationPropertiesScan
public class EsportsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsportsApplication.class, args);
    }
}
