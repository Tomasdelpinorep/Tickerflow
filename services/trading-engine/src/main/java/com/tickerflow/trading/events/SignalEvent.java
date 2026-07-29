package com.tickerflow.trading.events;

import com.tickerflow.trading.enums.SignalType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record SignalEvent(String symbol, String windowSize, SignalType signalType, double smaShort, double smaLong,
                          Instant timestamp) {
}
