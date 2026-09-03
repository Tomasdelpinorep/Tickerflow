package com.tickerflow.ingestion.publisher;

import com.tickerflow.events.TickEvent;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TickEventPublisher {
    private final KafkaTemplate<String, TickEvent> kafkaTemplate;
    private final String topic;

    public TickEventPublisher(KafkaTemplate<String, TickEvent> kafkaTemplate,
        @Value("${app.topics.raw-ticks}") String rawTicksTopic
    ){
        this.kafkaTemplate = kafkaTemplate;
        this.topic = rawTicksTopic;
    }

    // Forces the Kafka producer to initialize here, on a Spring-managed thread, instead of
    // lazily on the first publish() call — which happens on a java.net.http WebSocket callback
    // thread whose context classloader can't see Spring Boot's nested BOOT-INF/lib jars.
    // .close() on this handle is safe: DefaultKafkaProducerFactory returns a shared-producer
    // proxy that no-ops close() so the underlying producer stays open for real sends.
    @PostConstruct
    void warmUpProducer() {
        kafkaTemplate.getProducerFactory().createProducer().close();
    }

    public void publish(TickEvent tickEvent){
        kafkaTemplate.send(topic, tickEvent.getSymbol(), tickEvent);
    }
}
