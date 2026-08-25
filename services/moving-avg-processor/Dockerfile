FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY event-schemas ./event-schemas
RUN cd event-schemas && mvn install -DskipTests

COPY services/moving-avg-processor ./services/moving-avg-processor
RUN cd services/moving-avg-processor && mvn package -DskipTests

FROM eclipse-temurin:21-jre
COPY --from=builder /build/services/moving-avg-processor/target/*.jar /app.jar
ENTRYPOINT ["java", "-jar" ,"/app.jar"]