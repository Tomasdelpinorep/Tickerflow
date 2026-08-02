package com.tickerflow.trading.publishers;

import com.tickerflow.events.TradeEvent;
import com.tickerflow.trading.entities.Outbox;
import com.tickerflow.trading.entities.Trade;
import com.tickerflow.trading.repositories.OutboxRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
@Slf4j
public class OutboxPublisher {
    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;
    private final OutboxRepository outboxRepo;
    private final ObjectMapper objectMapper;
    private final String tradeEventsTopic;

    OutboxPublisher(KafkaTemplate<String, TradeEvent> kafkaTemplate, OutboxRepository outboxRepo,
                    ObjectMapper objectMapper,
                    @Value("${app.topics.trade-events}") String tradeEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
        this.tradeEventsTopic = tradeEventsTopic;
    }

    @Scheduled(fixedRate = 60000)
    public void publish() {
        List<Outbox> outboxes = outboxRepo.findByPublishedFalse();

        for (Outbox outbox : outboxes) {
            // Must serialize JSON into entity first, then to event
            Trade trade = objectMapper.readValue(outbox.getPayload(), Trade.class);
            TradeEvent event = TradeEvent.newBuilder()
                    .setId(trade.getId())
                    .setSymbol(trade.getSymbol())
                    .setQuantity(trade.getQuantity())
                    .setOpenPrice(trade.getOpenPrice().doubleValue())
                    .setClosePrice(trade.getClosePrice() != null ? trade.getClosePrice().doubleValue() : null)
                    .setPnl(trade.getPnl() != null ? trade.getPnl().doubleValue() : null)
                    .setStatus(trade.getStatus().name())
                    .setCreatedAt(trade.getCreatedAt())
                    .build();
            kafkaTemplate.send(tradeEventsTopic, event.getSymbol(), event);
            outbox.setPublished(true);
            outboxRepo.save(outbox);
        }
    }
}
