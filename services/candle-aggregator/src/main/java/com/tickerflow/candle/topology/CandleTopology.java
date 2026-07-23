package com.tickerflow.candle.topology;

import com.tickerflow.candle.event.CandleEvent;
import com.tickerflow.candle.event.TickEvent;
import com.tickerflow.candle.stream.CandleAggregate;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableKafkaStreams
@Slf4j
public class CandleTopology {
    private final String rawTicksTopic;
    private final String candlesTopic;

    public CandleTopology(@Value("${app.topics.raw-ticks}") String rawTicksTopic,
                          @Value("${app.topics.candles}") String candlesTopic) {
        this.rawTicksTopic = rawTicksTopic;
        this.candlesTopic = candlesTopic;
    }

    @Bean
    public KStream<String, TickEvent> buildTopology(StreamsBuilder builder) {
        KStream<String, TickEvent> tickStream = builder.stream(rawTicksTopic,
                Consumed.with(Serdes.String(), jsonSerde(TickEvent.class)));

        buildCandleStream(tickStream, Duration.ofMinutes(1), "1m", "candle-1m-store");
        buildCandleStream(tickStream, Duration.ofMinutes(5), "5m", "candle-5m-store");
        buildCandleStream(tickStream, Duration.ofHours(1), "1h", "candle-1h-store");

        return tickStream;
    }

    private void buildCandleStream(KStream<String, TickEvent> tickStream, Duration windowDuration, String windowSize, String storeName) {
        tickStream
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(windowDuration))
                .aggregate(
                        () -> null,
                        (symbol, tick, agg) -> agg == null
                                ? CandleAggregate.builder()
                                .open(tick.price())
                                .high(tick.price())
                                .low(tick.price())
                                .close(tick.price())
                                .volume(tick.volume())
                                .build()
                                : CandleAggregate.builder()
                                .open(agg.open())
                                .high(Math.max(tick.price(), agg.high()))
                                .low(Math.min(tick.price(), agg.low()))
                                .close(tick.price())
                                .volume(agg.volume() + tick.volume())
                                .build(),
                        Materialized.<String, CandleAggregate, WindowStore<Bytes, byte[]>>as(storeName)
                                .withValueSerde(jsonSerde(CandleAggregate.class)) // No need for key serde, default is declared in yml
                )
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                .map((windowedKey, agg) -> KeyValue.pair(
                        windowedKey.key(),
                        CandleEvent.builder()
                                .symbol(windowedKey.key())
                                .open(agg.open())
                                .high(agg.high())
                                .low(agg.low())
                                .close(agg.close())
                                .volume(agg.volume())
                                .windowSize(windowSize)
                                .windowStart(windowedKey.window().startTime())
                                .windowEnd(windowedKey.window().endTime())
                                .build()))
                .peek((key, candle) -> log.info("Candle closed: {} {} O:{} H:{} L:{} C:{}",
                        candle.windowSize(), candle.symbol(),candle.open(), candle.high(), candle.low(), candle.close()))
                .to(candlesTopic, Produced.valueSerde(jsonSerde(CandleEvent.class))); // No need for key serde, default is declared in yml
    }

    private <T> JacksonJsonSerde<T> jsonSerde(Class<T> clazz) {
        JacksonJsonSerde<T> serde = new JacksonJsonSerde<>(clazz);
        serde.configure(Map.of(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false), false);
        return serde;
    }
}
