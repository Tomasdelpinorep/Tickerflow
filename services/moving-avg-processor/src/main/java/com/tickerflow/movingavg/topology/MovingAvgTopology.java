package com.tickerflow.movingavg.topology;

import com.tickerflow.events.CandleEvent;
import com.tickerflow.events.SignalEvent;
import com.tickerflow.movingavg.stream.PriceBuffer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@EnableKafkaStreams
@Slf4j
public class MovingAvgTopology {
    private final String candlesTopic;
    private final String signalsTopic;
    private final String smaStoreName;

    MovingAvgTopology(@Value("${app.topics.candles}") String candlesTopic,
                      @Value("${app.topics.signals}") String signalsTopic,
                      @Value("${app.stores.sma}") String smaStoreName) {
        this.candlesTopic = candlesTopic;
        this.signalsTopic = signalsTopic;
        this.smaStoreName = smaStoreName;
    }

    @Bean
    public KStream<String, CandleEvent> buildTopology(StreamsBuilder builder) {
        KStream<String, CandleEvent> candleStream = builder.stream(candlesTopic);

        candleStream
                .selectKey((key, value) -> key + ":" + value.getWindowSize())
                .groupByKey()
                .aggregate(
                        () -> PriceBuffer.builder()
                                .closes(new ArrayList<>())
                                .prevSmaShort(0)
                                .prevSmaLong(0)
                                .signalType(null)
                                .build(),
                        (symbolWindow, candle, buffer) -> {
                            List<Double> closes = new ArrayList<>(buffer.closes());
                            closes.add(candle.getClose());
                            if (closes.size() > 50) closes.removeFirst();

                            if (closes.size() < 20) return PriceBuffer.builder()
                                    .closes(closes).prevSmaShort(0).prevSmaLong(0).signalType(null).build();

                            double smaShort = sma(closes, 20);
                            double smaLong = closes.size() >= 50 ? sma(closes, 50) : 0;

                            String signalType = null;
                            if (closes.size() >= 50) {
                                if (buffer.prevSmaShort() < buffer.prevSmaLong() && smaShort > smaLong) signalType = "BUY";
                                else if (buffer.prevSmaShort() > buffer.prevSmaLong() && smaShort < smaLong) signalType = "SELL";
                            }

                            return PriceBuffer.builder()
                                    .closes(closes).prevSmaShort(smaShort).prevSmaLong(smaLong).signalType(signalType).build();
                        },
                        Materialized.<String, PriceBuffer, KeyValueStore<Bytes, byte[]>>as(smaStoreName)
                                .withValueSerde(jsonSerde(PriceBuffer.class))
                )
                .toStream()
                .filter((key, buffer) -> buffer.signalType() != null)
                .map((key, buffer) -> KeyValue.pair(key,
                        SignalEvent.newBuilder()
                                .setSymbol(key.split(":")[0])
                                .setWindowSize(key.split(":")[1])
                                .setExecutePrice(buffer.closes().getLast())
                                .setSmaShort(buffer.prevSmaShort())
                                .setSmaLong(buffer.prevSmaLong())
                                .setSignalType(buffer.signalType())
                                .setTimestamp(Instant.now())
                                .build()))
                .peek((key, signal) -> log.info("Signal: {}", signal))
                .to(signalsTopic);

        return candleStream;
    }

    private double sma(List<Double> closes, int period) {
        if (closes.size() < period) return 0;
        return closes.subList(closes.size() - period, closes.size()).stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private <T> JacksonJsonSerde<T> jsonSerde(Class<T> clazz) {
        JacksonJsonSerde<T> serde = new JacksonJsonSerde<>(clazz);
        serde.configure(Map.of(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false), false);
        return serde;
    }
}
