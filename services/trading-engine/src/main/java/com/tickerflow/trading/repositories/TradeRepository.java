package com.tickerflow.trading.repositories;

import com.tickerflow.trading.entities.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    @Query(value = """
            SELECT * FROM trading.trades
            WHERE symbol = :symbol AND status = 'OPEN'
            ORDER BY created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<Trade> findLatestOpenTrade(@Param("symbol") String symbol);
}
