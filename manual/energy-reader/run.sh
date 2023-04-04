#!/bin/bash
export OTEL_SERVICE_NAME=WindReader
export OTEL_TRACES_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=none
export OTEL_LOGS_EXPORTER=none
java -Dotel.java.global-autoconfigure.enabled=true \
-jar ./target/EnergyReader-jar-with-dependencies.jar
