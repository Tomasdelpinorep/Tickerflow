 # Stage 1: build event-schemas, then the service, using Maven + JDK
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY event-schemas ./event-schemas
# goes into build/event-schemas and compiles, packages, and drops events-schemas-1.0.0.jar into this container's local .m2 cache
RUN cd event-schemas && mvn install -DskipTests

COPY services/ingestion-service ./services/ingestion-service
# here we package instead of install because nothing else depends on ingestion service via Maven, we only need the jar
RUN cd services/ingestion-service && mvn package -DskipTests

# Stage 2: lean runtime image — just the JRE + the built jar, nothing else
FROM eclipse-temurin:21-jre
COPY --from=builder /build/services/ingestion-service/target/*.jar /app.jar
# run java -jar /app.jar on container startup
ENTRYPOINT ["java", "-jar", "/app.jar"]