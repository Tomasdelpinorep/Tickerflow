FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY event-schemas ./event-schemas
RUN cd event-schemas && mvn install -DskipTests

COPY services/notification-service ./services/notification-service
RUN cd services/notification-service && mvn package -DskipTests

FROM eclipse-temurin:21-jre
COPY --from=builder /build/services/notification-service/target/*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
