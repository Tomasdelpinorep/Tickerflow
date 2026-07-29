package com.tickerflow.trading.entities;

import com.tickerflow.trading.enums.SignalType;
import com.tickerflow.trading.enums.TradeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "trades", schema = "trading")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type")
    private SignalType signalType;

    private int quantity;

    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private TradeStatus status;

    @Column(name = "created_at")
    private Instant createdAt;
}
