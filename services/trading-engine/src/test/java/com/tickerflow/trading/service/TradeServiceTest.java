package com.tickerflow.trading.service;

import com.tickerflow.events.SignalEvent;
import com.tickerflow.trading.entities.Outbox;
import com.tickerflow.trading.entities.Trade;
import com.tickerflow.trading.enums.OutboxEvents;
import com.tickerflow.trading.enums.TradeStatus;
import com.tickerflow.trading.repositories.OutboxRepository;
import com.tickerflow.trading.repositories.TradeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeServiceTest {

    @Mock TradeRepository tradeRepo;
    @Mock OutboxRepository outboxRepo;
    @Mock ObjectMapper objectMapper;

    @InjectMocks TradeService tradeService;

    // Tests that what gets saved when opening a trade after business logic runs is correct
    @Test
    void buySignal_createsOpenTradeAndOutboxEntry() {
        SignalEvent signal = buySignal("AAPL", 210.50);
        Trade persisted = openTrade(1L, "AAPL", new BigDecimal("210.50"));

        when(tradeRepo.save(any(Trade.class))).thenReturn(persisted);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        tradeService.executeTrade(signal);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepo).save(tradeCaptor.capture());
        Trade saved = tradeCaptor.getValue();
        assertThat(saved.getSymbol()).isEqualTo("AAPL");
        assertThat(saved.getOpenPrice()).isEqualByComparingTo(new BigDecimal("210.50"));
        assertThat(saved.getQuantity()).isEqualTo(10);
        assertThat(saved.getStatus()).isEqualTo(TradeStatus.OPEN);

        ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepo).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(OutboxEvents.TRADE_OPENED);
        assertThat(outboxCaptor.getValue().isPublished()).isFalse();
    }

    @Test
    void sellSignal_closesTradeAndCalculatesPnl() {
        SignalEvent signal = sellSignal("AAPL", 225.00);
        Trade openTrade = openTrade(1L, "AAPL", new BigDecimal("200.00"));

        when(tradeRepo.findLatestOpenTrade("AAPL")).thenReturn(Optional.of(openTrade));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        tradeService.executeTrade(signal);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepo).save(tradeCaptor.capture());
        Trade saved = tradeCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TradeStatus.CLOSED);
        assertThat(saved.getClosePrice()).isEqualByComparingTo(new BigDecimal("225.00"));
        assertThat(saved.getPnl()).isEqualByComparingTo(new BigDecimal("250.00")); // (225 - 200) * 10

        ArgumentCaptor<Outbox> outboxCaptor = ArgumentCaptor.forClass(Outbox.class);
        verify(outboxRepo).save(outboxCaptor.capture());
        assertThat(outboxCaptor.getValue().getEventType()).isEqualTo(OutboxEvents.TRADE_CLOSED);
        assertThat(outboxCaptor.getValue().isPublished()).isFalse();
    }

    @Test
    void sellSignal_withNoOpenTrade_isDiscarded() {
        SignalEvent signal = sellSignal("AAPL", 225.00);

        when(tradeRepo.findLatestOpenTrade("AAPL")).thenReturn(Optional.empty());

        tradeService.executeTrade(signal);

        verify(tradeRepo, never()).save(any());
        verify(outboxRepo, never()).save(any());
    }

    @Test
    void sellSignal_withLoss_calculatesNegativePnl() {
        SignalEvent signal = sellSignal("AAPL", 190.00);
        Trade openTrade = openTrade(1L, "AAPL", new BigDecimal("200.00"));

        when(tradeRepo.findLatestOpenTrade("AAPL")).thenReturn(Optional.of(openTrade));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        tradeService.executeTrade(signal);

        ArgumentCaptor<Trade> tradeCaptor = ArgumentCaptor.forClass(Trade.class);
        verify(tradeRepo).save(tradeCaptor.capture());
        assertThat(tradeCaptor.getValue().getPnl()).isEqualByComparingTo(new BigDecimal("-100.00")); // (190 - 200) * 10
    }

    // --- helpers ---

    private SignalEvent buySignal(String symbol, double price) {
        return SignalEvent.newBuilder()
                .setSymbol(symbol).setWindowSize("1m").setSignalType("BUY")
                .setExecutePrice(price).setSmaShort(0.0).setSmaLong(0.0)
                .setTimestamp(Instant.now()).build();
    }

    private SignalEvent sellSignal(String symbol, double price) {
        return SignalEvent.newBuilder()
                .setSymbol(symbol).setWindowSize("1m").setSignalType("SELL")
                .setExecutePrice(price).setSmaShort(0.0).setSmaLong(0.0)
                .setTimestamp(Instant.now()).build();
    }

    private Trade openTrade(Long id, String symbol, BigDecimal openPrice) {
        return Trade.builder()
                .id(id).symbol(symbol).quantity(10)
                .openPrice(openPrice).status(TradeStatus.OPEN)
                .createdAt(Instant.now()).build();
    }
}
