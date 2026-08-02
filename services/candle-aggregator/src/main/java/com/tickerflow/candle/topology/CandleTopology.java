package com.tickerflow.candle.topology;

import com.tickerflow.candle.stream.CandleAggregate;
import com.tickerflow.events.CandleEvent;
import com.tickerflow.events.TickEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableKafkaStreams
@Slf4j
public class CandleTopology {
    private final String rawTicksTopic;
    private final String candlesTopic;
    private final String candle1mStore;
    private final String candle5mStore;
    private final String candle1hStore;

    public CandleTopology(@Value("${app.topics.raw-ticks}") String rawTicksTopic,
                          @Value("${app.topics.candles}") String candlesTopic,
                          @Value("${app.stores.candle-1m}") String candle1mStore,
                          @Value("${app.stores.candle-5m}") String candle5mStore,
                          @Value("${app.stores.candle-1h}") String candle1hStore) {
        this.rawTicksTopic = rawTicksTopic;
        this.candlesTopic = candlesTopic;
        this.candle1mStore = candle1mStore;
        this.candle5mStore = candle5mStore;
        this.candle1hStore = candle1hStore;
    }

    @Bean
    public KStream<String, TickEvent> buildTopology(StreamsBuilder builder) {
        KStream<String, TickEvent> tickStream = builder.stream(rawTicksTopic, Consumed.with(Serdes.String(), null));

        buildCandleStream(tickStream, Duration.ofMinutes(1), "1m", candle1mStore);
        buildCandleStream(tickStream, Duration.ofMinutes(5), "5m", candle5mStore);
        buildCandleStream(tickStream, Duration.ofHours(1), "1h", candle1hStore);

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
                                .open(tick.getPrice())
                                .high(tick.getPrice())
                                .low(tick.getPrice())
                                .close(tick.getPrice())
                                .volume(tick.getVolume())
                                .build()
                                : CandleAggregate.builder()
                                .open(agg.open())
                                .high(Math.max(tick.getPrice(), agg.high()))
                                .low(Math.min(tick.getPrice(), agg.low()))
                                .close(tick.getPrice())
                                .volume(agg.volume() + tick.getVolume())
                                .build(),
                        Materialized.<String, CandleAggregate, WindowStore<Bytes, byte[]>>as(storeName)
                                .withValueSerde(jsonSerde(CandleAggregate.class))
                )
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
                .toStream()
                .map((windowedKey, agg) -> KeyValue.pair(
                        windowedKey.key(),
                        CandleEvent.newBuilder()
                                .setSymbol(windowedKey.key())
                                .setOpen(agg.open())
                                .setHigh(agg.high())
                                .setLow(agg.low())
                                .setClose(agg.close())
                                .setVolume(agg.volume())
                                .setWindowSize(windowSize)
                                .setWindowStart(windowedKey.window().startTime())
                                .setWindowEnd(windowedKey.window().endTime())
                                .build()))
                .peek((key, candle) -> log.info("Candle closed: {} {} O:{} H:{} L:{} C:{}",
                        candle.getWindowSize(), candle.getSymbol(), candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose()))
                .to(candlesTopic, Produced.with(Serdes.String(), null));
    }

    private <T> JacksonJsonSerde<T> jsonSerde(Class<T> clazz) {
        JacksonJsonSerde<T> serde = new JacksonJsonSerde<>(clazz);
        serde.configure(Map.of(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false), false);
        return serde;
    }
}
