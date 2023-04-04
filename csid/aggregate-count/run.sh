#!/bin/bash
export OTEL_SERVICE_NAME=AggregrateCount
export OTEL_TRACES_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=none
export OTEL_LOGS_EXPORTER=none
java -javaagent:/opt/otel/opentelemetry-javaagent.jar \
-Dotel.instrumentation.kafka.experimental-span-attributes=true \
-Dotel.instrumentation.common.experimental.suppress-messaging-receive-spans=true \
-Dotel.javaagent.extensions=/opt/otel/lineage-opentel-extensions-0.0.1-SNAPSHOT-all.jar \
-jar build/libs/kstreams-aggregating-count-standalone-0.0.1.jar configuration/dev.properties
