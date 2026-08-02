package com.tickerflow.notification.services;

import com.tickerflow.events.TradeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;

    EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, "from@test.com", "to@test.com");
    }

    @Test
    void openTrade_subjectSaysOpen_closePriceAndPnlAreDash() {
        TradeEvent trade = TradeEvent.newBuilder()
                .setId(1L)
                .setSymbol("AAPL")
                .setQuantity(10)
                .setOpenPrice(210.50)
                .setClosePrice(null)
                .setPnl(null)
                .setStatus("OPEN")
                .setCreatedAt(Instant.now())
                .build();

        emailService.sendTradeNotification(trade);

        SimpleMailMessage message = captureMessage();
        assertThat(message.getSubject()).contains("AAPL").contains("open");
        assertThat(message.getText()).contains("Open price: $210.5");
        assertThat(message.getText()).contains("Close price: —");
        assertThat(message.getText()).contains("PnL: —");
    }

    @Test
    void closedTrade_subjectSaysClosed_closePriceAndPnlArePresent() {
        TradeEvent trade = TradeEvent.newBuilder()
                .setId(1L)
                .setSymbol("AAPL")
                .setQuantity(10)
                .setOpenPrice(210.50)
                .setClosePrice(225.00)
                .setPnl(145.00)
                .setStatus("CLOSED")
                .setCreatedAt(Instant.now())
                .build();

        emailService.sendTradeNotification(trade);

        SimpleMailMessage message = captureMessage();
        assertThat(message.getSubject()).contains("AAPL").contains("closed");
        assertThat(message.getText()).contains("Close price: $225.0");
        assertThat(message.getText()).contains("PnL: $145.0");
    }

    private SimpleMailMessage captureMessage() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }
}
