package com.tickerflow.notification.services;

import com.tickerflow.notification.events.TradeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;
    private final String from;
    private final String to;

    EmailService(JavaMailSender mailSender, @Value("${app.mail.from}") String from,
                 @Value("${app.mail.to}") String to) {
        this.mailSender = mailSender;
        this.from = from;
        this.to = to;
    }

    public void sendTradeNotification(TradeEvent trade) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("TickerFlow: " + trade.signalType() + " " + trade.symbol());
        message.setText("Trade executed:\n\nSymbol: " + trade.symbol() +
                "\nType: " + trade.signalType() +
                "\nPrice: $" + trade.price() +
                "\nQuantity: " + trade.quantity() +
                "\nStatus: " + trade.status());
        mailSender.send(message);
        log.info("Email sent for trade {}", trade.id());
    }
}
