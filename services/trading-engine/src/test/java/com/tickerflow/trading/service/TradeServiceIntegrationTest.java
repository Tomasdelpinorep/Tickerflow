package com.tickerflow.trading.service;

import com.tickerflow.events.SignalEvent;
import com.tickerflow.trading.entities.Outbox;
import com.tickerflow.trading.entities.Trade;
import com.tickerflow.trading.enums.OutboxEvents;
import com.tickerflow.trading.enums.TradeStatus;
import com.tickerflow.trading.publishers.OutboxPublisher;
import com.tickerflow.trading.repositories.OutboxRepository;
import com.tickerflow.trading.repositories.TradeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@Transactional
class TradeServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withInitScript("create-schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9999");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    // prevents the @Scheduled outbox publisher from firing during tests
    @MockitoBean OutboxPublisher outboxPublisher;

    @Autowired TradeService tradeService;
    @Autowired TradeRepository tradeRepo;
    @Autowired OutboxRepository outboxRepo;

    @Test
    void buySignal_persistsOpenTradeAndOutboxEntry() {
        tradeService.executeTrade(buySignal("AAPL", 210.50));

        List<Trade> trades = tradeRepo.findAll();
        assertThat(trades).hasSize(1);
        Trade trade = trades.get(0);
        assertThat(trade.getSymbol()).isEqualTo("AAPL");
        assertThat(trade.getStatus()).isEqualTo(TradeStatus.OPEN);
        assertThat(trade.getOpenPrice()).isEqualByComparingTo(new BigDecimal("210.50"));
        assertThat(trade.getQuantity()).isEqualTo(10);

        List<Outbox> outboxes = outboxRepo.findAll();
        assertThat(outboxes).hasSize(1);
        assertThat(outboxes.get(0).getEventType()).isEqualTo(OutboxEvents.TRADE_OPENED);
        assertThat(outboxes.get(0).isPublished()).isFalse();
    }

    @Test
    void sellSignal_closesTradeAndPersistsPnl() {
        tradeRepo.save(Trade.builder()
                .symbol("AAPL").quantity(10)
                .openPrice(new BigDecimal("200.00"))
                .status(TradeStatus.OPEN)
                .createdAt(Instant.now())
                .build());

        tradeService.executeTrade(sellSignal("AAPL", 225.00));

        List<Trade> trades = tradeRepo.findAll();
        assertThat(trades).hasSize(1);
        Trade trade = trades.get(0);
        assertThat(trade.getStatus()).isEqualTo(TradeStatus.CLOSED);
        assertThat(trade.getClosePrice()).isEqualByComparingTo(new BigDecimal("225.00"));
        assertThat(trade.getPnl()).isEqualByComparingTo(new BigDecimal("250.00")); // (225 - 200) * 10

        List<Outbox> outboxes = outboxRepo.findAll();
        assertThat(outboxes).hasSize(1);
        assertThat(outboxes.get(0).getEventType()).isEqualTo(OutboxEvents.TRADE_CLOSED);
    }

    @Test
    void findLatestOpenTrade_returnsNewestByCreatedAt() {
        tradeRepo.save(Trade.builder()
                .symbol("AAPL").quantity(10)
                .openPrice(new BigDecimal("100.00"))
                .status(TradeStatus.OPEN)
                .createdAt(Instant.now().minusSeconds(60))
                .build());

        Trade newer = tradeRepo.save(Trade.builder()
                .symbol("AAPL").quantity(10)
                .openPrice(new BigDecimal("150.00"))
                .status(TradeStatus.OPEN)
                .createdAt(Instant.now())
                .build());

        Optional<Trade> found = tradeRepo.findLatestOpenTrade("AAPL");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(newer.getId());
        assertThat(found.get().getOpenPrice()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

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
}
