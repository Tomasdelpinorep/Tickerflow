Seed one or more Avro SignalEvents into the `signals` Kafka topic for end-to-end testing.

Arguments: `$ARGUMENTS` — a comma-separated list of signals in the format `SYMBOL TYPE PRICE` (e.g. `AAPL BUY 210.50, MSFT SELL 418.00`). If no arguments are provided, ask the user what to seed.

For each signal, run this Bash command (substituting symbol, signalType, executePrice, and a current epoch-millis timestamp):

```bash
echo '{"symbol":"<SYMBOL>","windowSize":"1m","signalType":"<TYPE>","executePrice":<PRICE>,"smaShort":0.0,"smaLong":0.0,"timestamp":<EPOCH_MILLIS>}' | docker exec -i tickerflow-schema-registry kafka-avro-console-producer \
  --bootstrap-server kafka:29092 \
  --topic signals \
  --property schema.registry.url=http://localhost:8081 \
  --property avro.use.logical.type.converters=true \
  --property value.schema='{"type":"record","name":"SignalEvent","namespace":"com.tickerflow.events","fields":[{"name":"symbol","type":"string"},{"name":"windowSize","type":"string"},{"name":"signalType","type":"string"},{"name":"executePrice","type":"double"},{"name":"smaShort","type":"double"},{"name":"smaLong","type":"double"},{"name":"timestamp","type":{"type":"long","logicalType":"timestamp-millis"}}]}'
```

Use `date +%s%3N` to get the current epoch milliseconds for the timestamp field.

After all signals are sent, summarise what was seeded and remind the user that the outbox publisher fires every 60s before trade events reach the notification service.
