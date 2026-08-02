package com.tickerflow.trading.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import org.apache.commons.lang3.EnumUtils;

public enum SignalType {
    BUY, SELL,

    @JsonEnumDefaultValue
    UNKNOWN;

    public static SignalType fromString(String signalType) {
        return EnumUtils.getEnumIgnoreCase(SignalType.class, signalType);
    }
}
