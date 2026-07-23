package com.tickerflow.candle.event;

import lombok.Builder;

import java.time.Instant;

@Builder
public record TickEvent(String symbol, double price, double volume, Instant timestamp) {}