FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY event-schemas ./event-schemas
RUN cd event-schemas && mvn install -DskipTests

COPY services/trading-engine ./services/trading-engine
RUN cd services/trading-engine && mvn package -DskipTests

FROM eclipse-temurin:21-jre
COPY --from=builder /build/services/trading-engine/target/*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]