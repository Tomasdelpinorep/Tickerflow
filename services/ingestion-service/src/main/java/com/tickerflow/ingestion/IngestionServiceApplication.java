package com.tickerflow.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.tickerflow.ingestion.config.FinnhubProperties;

@SpringBootApplication
@EnableConfigurationProperties(FinnhubProperties.class)
public class IngestionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(IngestionServiceApplication.class, args);
	}

}
