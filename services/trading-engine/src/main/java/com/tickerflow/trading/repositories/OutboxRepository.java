package com.tickerflow.trading.repositories;

import com.tickerflow.trading.entities.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    List<Outbox> findByPublishedFalse();
}
