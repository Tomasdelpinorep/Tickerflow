package com.tickerflow.ingestion.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Listener;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.tickerflow.ingestion.config.FinnhubProperties;
import com.tickerflow.events.TickEvent;
import com.tickerflow.ingestion.publisher.TickEventPublisher;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


@Slf4j
@Service
public class FinnhubWebSocketClient implements WebSocket.Listener {
    private final FinnhubProperties properties;
    private final TickEventPublisher publisher;
    private final ObjectMapper objectMapper;

    public FinnhubWebSocketClient(FinnhubProperties properties,
                                  TickEventPublisher publisher,
                                  ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connect() {
        HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(URI.create("wss://ws.finnhub.io?token=" + properties.apiKey()), this)
                .join();
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("WebSocket error", error);

        Listener.super.onError(webSocket, error);
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        CompletableFuture<WebSocket> chain = CompletableFuture.completedFuture(webSocket);

        for (String symbol : properties.symbols()) {
            String data = "{\"type\":\"subscribe\",\"symbol\":\"" + symbol + "\"}";
            chain = chain.thenCompose(ws -> ws.sendText(data, true)); // chain sends sequentially, waiting for each to complete before the next
        }

        log.info("Connected to Finnhub, subscribing to {} symbols", properties.symbols().size());
        Listener.super.onOpen(webSocket); //webSocket.request(1); Marks websocket as ready to receive 1 request
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        try {
            JsonNode root = objectMapper.readTree(data.toString());
            log.info("Received message from Finnhub: type={}", root.get("type").asString());

            if (!root.get("type").asString().equalsIgnoreCase("trade")){
                return Listener.super.onText(webSocket, data, last);
            }

            for (JsonNode tickJSON : root.get("data")) {
                TickEvent tick = TickEvent.newBuilder()
                        .setSymbol(tickJSON.get("s").asString())
                        .setPrice(tickJSON.get("p").asDouble())
                        .setVolume(tickJSON.get("v").asDouble())
                        .setTimestamp(Instant.ofEpochMilli(tickJSON.get("t").asLong()))
                        .build();

                publisher.publish(tick);
            }
        } catch (Exception e) {
            log.error("Failed to process tick message: {}", data, e);
        }

        return Listener.super.onText(webSocket, data, last);
    }


}
