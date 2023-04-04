#!/bin/bash
export OTEL_SERVICE_NAME=WindGenerator
export OTEL_TRACES_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=none
export OTEL_LOGS_EXPORTER=none
java -Dotel.java.global-autoconfigure.enabled=true \
-Dotel.instrumentation.kafka.experimental-span-attributes=true \
-Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=true \
-Dotel.javaagent.extensions=/opt/otel/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
-jar ./target/EnergyGenerator-jar-with-dependencies.jar
