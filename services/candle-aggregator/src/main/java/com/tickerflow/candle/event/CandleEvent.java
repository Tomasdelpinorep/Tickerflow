package com.tickerflow.candle.event;

import lombok.Builder;

import java.time.Instant;

@Builder
public record CandleEvent(String symbol, double open, double high, double low, double close, double volume,
                          Instant windowStart, Instant windowEnd, String windowSize) {
};
