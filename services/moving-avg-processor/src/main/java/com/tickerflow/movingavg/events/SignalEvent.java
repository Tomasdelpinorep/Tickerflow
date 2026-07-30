package com.tickerflow.movingavg.events;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record SignalEvent(String symbol, String windowSize, String signalType, BigDecimal executePrice, double smaShort,
                          double smaLong, Instant timestamp) {
}
