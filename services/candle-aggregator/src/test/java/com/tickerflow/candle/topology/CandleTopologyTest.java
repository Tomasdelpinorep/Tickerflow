package com.tickerflow.candle.topology;

import com.tickerflow.events.CandleEvent;
import com.tickerflow.events.TickEvent;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class CandleTopologyTest {

    private static final String MOCK_REGISTRY = "mock://candle-test";

    private TopologyTestDriver driver;
    private TestInputTopic<String, TickEvent> tickInput;
    private TestOutputTopic<String, CandleEvent> candleOutput;

    @BeforeEach
    void setUp() {
        // Config map to point to mock candle schema registry, needed since tests are all in-memory, not docker
        Map<String, String> srConfig = Map.of(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_REGISTRY
        );

        SpecificAvroSerde<TickEvent> tickSerde = new SpecificAvroSerde<>();
        tickSerde.configure(srConfig, false);

        SpecificAvroSerde<CandleEvent> candleSerde = new SpecificAvroSerde<>();
        candleSerde.configure(srConfig, false);

        StreamsBuilder builder = new StreamsBuilder();
        // if we actually need to send the store strings, what purpose are the @value tags?
        new CandleTopology("raw-ticks", "candles", "candle-1m", "candle-5m", "candle-1h")
                .buildTopology(builder);

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "candle-test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class.getName());
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_REGISTRY);
        props.put("specific.avro.reader", "true");

        driver = new TopologyTestDriver(builder.build(), props);
        tickInput = driver.createInputTopic("raw-ticks", Serdes.String().serializer(), tickSerde.serializer());
        candleOutput = driver.createOutputTopic("candles", Serdes.String().deserializer(), candleSerde.deserializer());
    }

    @AfterEach
    void tearDown() {
        driver.close();
    }

    @Test
    void ticksWithinWindow_aggregateIntoCorrectCandle() {
        Instant t0 = Instant.parse("2014-10-28T12:00:00Z");

        tickInput.pipeInput("AAPL", tick("AAPL", 100.0, 10.0), t0);
        tickInput.pipeInput("AAPL", tick("AAPL", 105.0, 20.0), t0.plusSeconds(30));
        tickInput.pipeInput("AAPL", tick("AAPL", 98.0,  15.0), t0.plusSeconds(59));
        tickInput.pipeInput("AAPL", tick("AAPL", 99.0,   5.0), t0.plusMillis(60001));

        CandleEvent candle = candleOutput.readValue();
        assertThat(candle.getSymbol()).isEqualTo("AAPL");
        assertThat(candle.getOpen()).isEqualTo(100.0);
        assertThat(candle.getHigh()).isEqualTo(105.0);
        assertThat(candle.getLow()).isEqualTo(98.0);
        assertThat(candle.getClose()).isEqualTo(98.0);
        assertThat(candle.getVolume()).isEqualTo(45.0);
        assertThat(candle.getWindowSize()).isEqualTo("1m");
    }

    @Test
    void singleTick_allOhlcAreEqual() {
        Instant t0 = Instant.parse("2024-01-01T10:00:00Z");

        tickInput.pipeInput("AAPL", tick("AAPL", 150.0, 5.0), t0);
        tickInput.pipeInput("AAPL", tick("AAPL", 150.0, 5.0), t0.plusMillis(60001));

        CandleEvent candle = candleOutput.readValue();
        assertThat(candle.getOpen()).isEqualTo(150.0);
        assertThat(candle.getHigh()).isEqualTo(150.0);
        assertThat(candle.getLow()).isEqualTo(150.0);
        assertThat(candle.getClose()).isEqualTo(150.0);
    }

    @Test
    void windowNotYetClosed_noCandleEmitted() {
        Instant t0 = Instant.parse("2024-01-01T10:00:00Z");

        tickInput.pipeInput("AAPL", tick("AAPL", 100.0, 10.0), t0);
        tickInput.pipeInput("AAPL", tick("AAPL", 102.0,  5.0), t0.plusSeconds(30));

        assertThat(candleOutput.isEmpty()).isTrue();
    }

    private TickEvent tick(String symbol, double price, double volume) {
        return TickEvent.newBuilder()
                .setSymbol(symbol)
                .setPrice(price)
                .setVolume(volume)
                .setTimestamp(Instant.now())
                .build();
    }
}
