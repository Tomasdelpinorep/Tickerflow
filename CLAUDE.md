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
- AWS (EC2, RDS, ECR, VPC via Terraform) — **phase 2 only, after local works end-to-end**. Actual services used are driven by the cost constraints below, not a fixed target architecture — e.g. MSK/ECS Fargate are not assumed just because they were an early idea.
- Confluent Cloud (Basic cluster) for Kafka in the AWS deployment — see Cost constraints below for why.
- Finnhub WebSocket API (free tier) for live price ticks

## Cost constraints (AWS phase)

This is a personal portfolio project, not funded infra — the goal is to keep AWS spend as close to **$0/month** as possible, and never pay for HA/scale this project doesn't need to demonstrate.

- For every infra choice with a paid alternative, briefly document the option considered and *why*, cost included — this is as much an interview talking point as the architecture patterns are.
- Default to the free-tier or $0-marginal-cost option unless there's a concrete reason not to (e.g. a pattern the role explicitly needs to demonstrate).
- Don't assume production-grade HA/redundancy is required — this project has no uptime SLA and near-zero real traffic. A single point of failure that would be a real problem in production may be an acceptable, explicitly-documented tradeoff here.
- **Decided: Kafka runs on Confluent Cloud (Basic cluster)**, not self-hosted on the EC2 instance. The "self-hosting is free, the box is already paid for" assumption didn't survive a real memory audit: the 5 Spring Boot services + Kafka broker + Schema Registry all need to fit on a single `t3.small` (2GiB RAM), and candle-aggregator/moving-avg-processor's Kafka Streams state stores add 12 RocksDB instances (9 + 3, one per store per partition) whose off-heap memory — up to ~1.2GB at Kafka Streams' defaults — isn't reducible by JVM tuning. Closing that gap meant either upgrading to `t3.medium` (+$15.19/mo) or GraalVM native-image (rejected — RocksDB's off-heap memory isn't touched by native-image at all since it's native memory via JNI, and `rocksdbjni`'s native-image compatibility is unresolved upstream). Confluent Cloud Basic's actual usage-based cost for our real 10-symbol traffic came out to ~$0.30–$1.10/month — cheaper than the instance upgrade, with none of the compatibility risk. AWS MSK was ruled out separately (no free tier, real ongoing hourly cost regardless of usage). One more self-hosting lever exists — bounding RocksDB's per-instance memory via a shared `RocksDBConfigSetter` (block cache + write buffer manager) instead of each of the 12 stores defaulting its own — but it was deliberately not pursued: even a fully successful outcome only closes the gap to Confluent Cloud's ~$1/month, which doesn't justify the engineering time (containerizing 2 more services, constrained-memory measurement, tuning iteration) to chase it. Revisit only if Confluent Cloud pricing changes materially.

## Build order (current phase: local-only, Docker Compose)

1. Docker Compose environment (Kafka in KRaft mode, Postgres)
2. Ingestion Service — Finnhub WS client → Kafka producer
3. Candle aggregation via Kafka Streams (1m/5m/1h windows)
4. Moving averages (SMA/EMA) + crossover signal detection
5. Trading Engine — paper trades + outbox pattern
6. Notification Service
7. Schema Registry (Avro)
8. AWS deployment (EC2, RDS, Confluent Cloud for Kafka, Terraform)
9. Observability (CloudWatch, X-Ray)

**Do not jump ahead to AWS deployment until the full pipeline works locally in Docker Compose.**

## Working style

- Act as a teacher, don't tell me the next thing to do, instead always ask me what I think the next step is, identify the gaps in my knowledge and help me fill them
- The goal is make me able to create this project without any help from AI. Tell me how a senior developer would do it, how and where to look up the correct documentation, guide me on how to read it.
- I prefer concise answers/explanations, not walls of text
- Explain the *why* behind architectural decisions, especially Kafka/streaming ones — I want to be able to defend every choice in an interview
- Push back if I ask for something that isn't actually a good practice — don't just implement blindly
- Small, reviewable commits over big dumps of code
