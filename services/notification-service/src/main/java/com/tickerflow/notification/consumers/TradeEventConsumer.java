package com.tickerflow.notification.consumers;

import com.tickerflow.events.TradeEvent;
import com.tickerflow.notification.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TradeEventConsumer {
    private final EmailService  emailService;

    TradeEventConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics =  "${app.topics.trade-events}")
    public void consume(TradeEvent trade) {
        log.info("Trade event received: {}", trade);
        emailService.sendTradeNotification(trade);
    }
}
