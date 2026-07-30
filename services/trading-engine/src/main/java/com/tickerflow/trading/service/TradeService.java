package com.tickerflow.trading.service;

import com.tickerflow.trading.constants.TradeConstants;
import com.tickerflow.trading.entities.Outbox;
import com.tickerflow.trading.entities.Trade;
import com.tickerflow.trading.enums.OutboxEvents;
import com.tickerflow.trading.enums.SignalType;
import com.tickerflow.trading.enums.TradeStatus;
import com.tickerflow.trading.events.SignalEvent;
import com.tickerflow.trading.repositories.OutboxRepository;
import com.tickerflow.trading.repositories.TradeRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
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
        if (signal.signalType().equals(SignalType.SELL)) {
            Optional<Trade> latestOpenTrade = tradeRepo.findLatestOpenTrade(signal.symbol());

            if (latestOpenTrade.isEmpty()) {
                log.warn("Discarding SELL signal for {}: no open trade to close", signal.symbol());
                return;
            }

            Trade latest = latestOpenTrade.get();
            latest.setClosePrice(signal.executePrice());
            BigDecimal priceDiff = latest.getClosePrice().subtract(latest.getOpenPrice());
            BigDecimal pnl = priceDiff.multiply(BigDecimal.valueOf(latest.getQuantity()));
            latest.setPnl(pnl);
            latest.setStatus(TradeStatus.CLOSED);
            tradeRepo.save(latest);
            saveOutboxEvent(OutboxEvents.TRADE_CLOSED, latest);
        } else {
            Trade trade = tradeRepo.save(Trade.builder()
                    .symbol(signal.symbol())
                    .signalType(signal.signalType())
                    .quantity(TradeConstants.DEFAULT_TRADE_QUANTITY)
                    .openPrice(signal.executePrice())
                    .status(TradeStatus.OPEN)
                    .createdAt(Instant.now())
                    .build());
            saveOutboxEvent(OutboxEvents.TRADE_OPENED, trade);
        }
    }

    private void saveOutboxEvent(OutboxEvents eventType, Trade trade) {
        outboxRepo.save(Outbox.builder()
                .eventType(eventType)
                .payload(objectMapper.writeValueAsString(trade))
                .createdAt(Instant.now())
                .published(false)
                .build());
    }
}
