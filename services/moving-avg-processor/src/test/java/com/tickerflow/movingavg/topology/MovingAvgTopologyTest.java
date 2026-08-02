package com.tickerflow.movingavg.topology;

import com.tickerflow.events.CandleEvent;
import com.tickerflow.events.SignalEvent;
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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class MovingAvgTopologyTest {

    private static final String MOCK_REGISTRY = "mock://movingavg-test";

    private TopologyTestDriver driver;
    private TestInputTopic<String, CandleEvent> candleInput;
    private TestOutputTopic<String, SignalEvent> signalOutput;

    @BeforeEach
    void setUp() {
        Map<String, String> srConfig = Map.of(
                AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_REGISTRY
        );

        SpecificAvroSerde<CandleEvent> candleSerde = new SpecificAvroSerde<>();
        candleSerde.configure(srConfig, false);

        SpecificAvroSerde<SignalEvent> signalSerde = new SpecificAvroSerde<>();
        signalSerde.configure(srConfig, false);

        StreamsBuilder builder = new StreamsBuilder();
        new MovingAvgTopology("candles", "signals", "sma-store")
                .buildTopology(builder);

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "movingavg-test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class.getName());
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, MOCK_REGISTRY);
        props.put("specific.avro.reader", "true");

        driver = new TopologyTestDriver(builder.build(), props);
        candleInput = driver.createInputTopic("candles", Serdes.String().serializer(), candleSerde.serializer());
        signalOutput = driver.createOutputTopic("signals", Serdes.String().deserializer(), signalSerde.deserializer());
    }

    @AfterEach
    void tearDown() {
        driver.close();
    }

    @Test
    void insufficientData_noSignalEmitted() {
        sendCandles("AAPL", 49, 100.0);

        assertThat(signalOutput.isEmpty()).isTrue();
    }

    @Test
    void sellCrossover_emitsSignal() {
        // 30 candles at 100 then 20 at 50 — at candle 50:
        //   smaShort = avg(last 20) = 50.0
        //   smaLong  = avg(all 50)  = (30*100 + 20*50) / 50 = 80.0
        //   smaShort < smaLong → SELL
        sendCandles("AAPL", 30, 100.0);
        sendCandles("AAPL", 20, 50.0);

        List<SignalEvent> signals = signalOutput.readValuesToList();
        assertThat(signals).hasSize(1);
        assertThat(signals.get(0).getSignalType()).isEqualTo("SELL");
        assertThat(signals.get(0).getSymbol()).isEqualTo("AAPL");
        assertThat(signals.get(0).getWindowSize()).isEqualTo("1m");
    }

    @Test
    void buyCrossover_emitsSignal() {
        // Same setup as SELL, then 1 spike candle at 2000:
        //   smaShort = avg(last 20) = (19*50 + 2000) / 20 = 147.5
        //   smaLong  = avg(all 50)  = (29*100 + 20*50 + 2000) / 50 = 118.0
        //   smaShort > smaLong, and prev was smaShort < smaLong → BUY
        sendCandles("AAPL", 30, 100.0);
        sendCandles("AAPL", 20, 50.0);
        sendCandles("AAPL", 1, 2000.0);

        List<SignalEvent> signals = signalOutput.readValuesToList();
        assertThat(signals).hasSize(2);
        assertThat(signals.get(0).getSignalType()).isEqualTo("SELL");
        assertThat(signals.get(1).getSignalType()).isEqualTo("BUY");
    }

    private void sendCandles(String symbol, int count, double close) {
        for (int i = 0; i < count; i++) {
            candleInput.pipeInput(symbol, candle(symbol, close));
        }
    }

    private CandleEvent candle(String symbol, double close) {
        return CandleEvent.newBuilder()
                .setSymbol(symbol)
                .setOpen(close).setHigh(close).setLow(close).setClose(close)
                .setVolume(1.0)
                .setWindowSize("1m")
                .setWindowStart(Instant.now())
                .setWindowEnd(Instant.now().plusSeconds(60))
                .build();
    }
}
