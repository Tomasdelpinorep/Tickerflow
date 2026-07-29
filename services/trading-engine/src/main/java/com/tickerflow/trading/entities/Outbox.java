package com.tickerflow.trading.entities;

import com.tickerflow.trading.enums.OutboxEvents;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "outbox" , schema = "trading")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Outbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private OutboxEvents eventType;

    private String payload;

    private boolean published;

    @Column(name = "created_at")
    private Instant createdAt;

}
