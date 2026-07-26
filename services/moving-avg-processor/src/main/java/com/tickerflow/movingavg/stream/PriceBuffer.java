package com.tickerflow.movingavg.stream;

import lombok.Builder;

import java.util.List;

@Builder
public record PriceBuffer(List<Double> closes, double prevSmaShort, double prevSmaLong, String signalType) {
}
