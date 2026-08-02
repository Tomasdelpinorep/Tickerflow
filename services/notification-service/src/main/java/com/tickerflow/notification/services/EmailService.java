package com.tickerflow.notification.services;

import com.tickerflow.events.TradeEvent;
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
        message.setSubject("TickerFlow: " + trade.getSymbol() + " position " + trade.getStatus().toLowerCase());
        message.setText("Trade update:\n\nSymbol: " + trade.getSymbol() +
                "\nStatus: " + trade.getStatus() +
                "\nOpen price: $" + trade.getOpenPrice() +
                "\nClose price: " + (trade.getClosePrice() != null ? "$" + trade.getClosePrice() : "—") +
                "\nPnL: " + (trade.getPnl() != null ? "$" + trade.getPnl() : "—") +
                "\nQuantity: " + trade.getQuantity());
        mailSender.send(message);
        log.info("Email sent for trade {}", trade.getId());
    }
}
