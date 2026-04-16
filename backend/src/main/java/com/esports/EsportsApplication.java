package com.esports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

// E-sports 팬 사이트 메인 애플리케이션 진입점
// @EnableScheduling: PandaScore 폴링 및 AI 요약 큐 처리 스케줄러 활성화
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class EsportsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsportsApplication.class, args);
    }
}
