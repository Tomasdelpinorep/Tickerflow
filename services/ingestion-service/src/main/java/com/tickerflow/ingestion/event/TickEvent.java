package com.tickerflow.ingestion.event;

import java.time.Instant;

import lombok.Builder;

@Builder
public record TickEvent(String symbol, double price, double volume, Instant timestamp) {}