package com.tickerflow.ingestion.publisher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.tickerflow.ingestion.event.TickEvent;

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

    public void publish(TickEvent tickEvent){
        kafkaTemplate.send(topic, tickEvent.symbol(), tickEvent);
    }
}
