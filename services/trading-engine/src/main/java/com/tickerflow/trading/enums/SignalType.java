package com.tickerflow.trading.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum SignalType {
    BUY, SELL,

    @JsonEnumDefaultValue
    UNKNOWN;
}
