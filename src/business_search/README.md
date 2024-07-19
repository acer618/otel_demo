# java_otel

## Subprojects

### Propagation
  - Manual Instrumentation
  - Exports to console and Jaeger (localhost:4318)
    
  - Run
    ```
    - (Optional) $sudo docker run --name jaeger -e COLLECTOR_OTLP_ENABLED=true   -p 16686:16686   -p 4317:4317   -p 4318:4318   jaegertracing/all-in-one:1.35 
    - $./gradlew shadowJar
    - $cd propagation
    - ./server.sh
    - ./client.sh
    ```
