# TickerFlow

## What this is

Event-driven stock market data pipeline, built as a portfolio project to get a **backend software engineer** role. The point is not the trading logic — it's demonstrating production-grade distributed backend architecture: event-driven microservices, stream processing, and cloud deployment.

## My background (so you calibrate explanations)

Full Stack Developer, Java/Spring Boot/Liferay 7.4/Angular/PostgreSQL day-to-day. Just passed AWS SAA-C03. Comfortable with Spring Boot, JPA/Hibernate, Spring Security, SQL. **New territory: Kafka Streams, event-driven microservices patterns (saga, outbox), stream processing.** Don't over-explain Spring Boot basics — do explain Kafka/streaming concepts in depth.

## Architecture

```
Finnhub WebSocket → Ingestion Service → Kafka (raw-ticks)
                                              ↓
                          ┌───────────────────┴───────────────────┐
                    Candle Aggregator                    Moving Avg Processor
                    (Kafka Streams)                       (Kafka Streams)
                          ↓                                       ↓
                   Kafka (candles)                        Kafka (signals)
                          └───────────────────┬───────────────────┘
                                       Trading Engine
                                    (paper trades, outbox)
                                              ↓
                                  Kafka (trade-events)
                                              ↓
                                   Notification Service
```

4 services, each owns its own Postgres schema, communicate only via Kafka topics — never direct REST calls between them.

## Patterns to implement deliberately (these are the interview talking points)

- **Saga pattern** (choreography-based) for the trade execution flow, with compensating events on failure
- **Outbox pattern** — transactional outbox table to avoid the dual-write problem (DB write + Kafka publish)
- **Idempotent consumers** — dedupe on event ID (Kafka is at-least-once delivery)
- **Dead letter topics** for poison messages
- **Schema Registry (Avro)** — versioned event contracts between services

## Tech Stack

- Java, Spring Boot
- Apache Kafka + Kafka Streams
- PostgreSQL (schema-per-service)
- Docker Compose for local dev
- AWS MSK, ECS Fargate, RDS, Terraform/CDK — **phase 2 only, after local works end-to-end**
- Finnhub WebSocket API (free tier) for live price ticks

## Build order (current phase: local-only, Docker Compose)

1. Docker Compose environment (Kafka in KRaft mode, Postgres)
2. Ingestion Service — Finnhub WS client → Kafka producer
3. Candle aggregation via Kafka Streams (1m/5m/1h windows)
4. Moving averages (SMA/EMA) + crossover signal detection
5. Trading Engine — paper trades + outbox pattern
6. Notification Service
7. Schema Registry (Avro)
8. AWS deployment (MSK, ECS, RDS, IaC)
9. Observability (CloudWatch, X-Ray)

**Do not jump ahead to AWS deployment until the full pipeline works locally in Docker Compose.**

## Working style

- I prefer concise answers/explanations, not walls of text
- Explain the *why* behind architectural decisions, especially Kafka/streaming ones — I want to be able to defend every choice in an interview
- Push back if I ask for something that isn't actually a good practice — don't just implement blindly
- Small, reviewable commits over big dumps of code