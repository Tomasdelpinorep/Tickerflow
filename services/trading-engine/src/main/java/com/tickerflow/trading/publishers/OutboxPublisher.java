package com.tickerflow.trading.publishers;

import com.tickerflow.trading.entities.Outbox;
import com.tickerflow.trading.repositories.OutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxRepository outboxRepo;
    private final String tradeEventsTopic;

    OutboxPublisher(KafkaTemplate<String, String> kafkaTemplate, OutboxRepository outboxRepo,
                    @Value("${app.topics" + ".trade-events}") String tradeEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.outboxRepo = outboxRepo;
        this.tradeEventsTopic = tradeEventsTopic;
    }

    @Scheduled(fixedRate = 60000)
    public void publish() {
        List<Outbox> outboxes = outboxRepo.findByPublishedFalse();

        for (Outbox outbox : outboxes) {
            kafkaTemplate.send(tradeEventsTopic, outbox.getPayload());
            outbox.setPublished(true);
            outboxRepo.save(outbox);
        }
    }
}
