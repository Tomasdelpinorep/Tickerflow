package com.tickerflow.trading.consumers;

import com.tickerflow.events.SignalEvent;
import com.tickerflow.trading.service.TradeService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class SignalConsumer {
    private final TradeService tradeService;

    SignalConsumer(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    // @KafkaListener deserializes automatically via spring.kafka.consumer properties in application.yml.
    // Kafka Streams has no yaml hook for serde config, so jsonSerde() configures it programmatically there.
    @KafkaListener(topics = "${app.topics.signals}")
    public void consume(SignalEvent signal) {
        tradeService.executeTrade(signal);
    }
}
