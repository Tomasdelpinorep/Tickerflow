package com.tickerflow.candle.stream;

import lombok.Builder;

@Builder
public record CandleAggregate(double open, double high, double low, double close, double volume) {
}
