#!/bin/bash
export OTEL_SERVICE_NAME=EnergyGenerator
export OTEL_TRACES_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=none
export OTEL_LOGS_EXPORTER=none
java -javaagent:/opt/otel/opentelemetry-javaagent.jar \
-jar ./target/EnergyGenerator-jar-with-dependencies.jar
