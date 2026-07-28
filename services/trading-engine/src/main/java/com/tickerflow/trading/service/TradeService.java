package com.tickerflow.trading.service;

import com.tickerflow.trading.constants.TradeConstants;
import com.tickerflow.trading.entities.Outbox;
import com.tickerflow.trading.entities.Trade;
import com.tickerflow.trading.events.SignalEvent;
import com.tickerflow.trading.repositories.OutboxRepository;
import com.tickerflow.trading.repositories.TradeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class TradeService {
    private final TradeRepository tradeRepo;
    private final OutboxRepository outboxRepo;
    private final ObjectMapper objectMapper;

    TradeService(TradeRepository tradeRepo, OutboxRepository outboxRepo, ObjectMapper objectMapper) {
        this.tradeRepo = tradeRepo;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void executeTrade(SignalEvent signal) {
        Trade trade = tradeRepo.save(Trade.builder()
                .symbol(signal.symbol())
                .signalType(signal.signalType())
                .quantity(TradeConstants.DEFAULT_TRADE_QUANTITY)
                .price(BigDecimal.valueOf(signal.smaShort()))
                .status("OPEN")
                .createdAt(Instant.now())
                .build());

        outboxRepo.save(Outbox.builder()
                .eventType(signal.signalType())
                .payload(objectMapper.writeValueAsString(trade))
                .createdAt(Instant.now())
                .published(false)
                .build());
    }
}
