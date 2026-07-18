package com.tickerflow.ingestion.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "finnhub")
public record FinnhubProperties(String apiKey, List<String> symbols) {}
