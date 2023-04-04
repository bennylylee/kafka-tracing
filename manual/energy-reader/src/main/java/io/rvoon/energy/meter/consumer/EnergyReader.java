package io.rvoon.energy.meter.consumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.KafkaTelemetry;

public class EnergyReader {

    private static final Logger log = LoggerFactory.getLogger(EnergyReader.class);
    
    static final OpenTelemetry otel = GlobalOpenTelemetry.get();
    static final Tracer tracer = otel.getTracer("io.rvoon.energy.meter", "1.0.0");
    
    private static void printRecord(ConsumerRecord<String, Integer> record) {
        Span span = tracer.spanBuilder("Print Energy Record").startSpan();
        try (Scope scope = span.makeCurrent()) {
            log.info("Received record:");
            log.info("\tTopic = " + record.topic());
            log.info("\tPartition = " + record.partition());
            log.info("\tKey = " + record.key());
            log.info("\tValue = " + record.value());
        } finally {
            span.end();
        }
    }

    private static void printAggregation(int aggregationResult) {
        log.info("Writing aggregation result to file: " + aggregationResult);
    }

    private static void saveAggregationToFile(int aggregationResult) throws IOException {
        Path reportFile = Path.of("report.txt");
        Files.writeString(reportFile, Integer.toString(aggregationResult));
    }

    private static Properties configureProperties() throws IOException {
        final Properties envProps = new Properties();
        envProps.load(EnergyReader.class.getResourceAsStream("/consumer.properties"));
        return envProps;
    }

    /* 
    private static void sampleSpan() {
        
        Span childSpan = tracer.spanBuilder("Doing Something Important").startSpan();
        try (Scope scope = childSpan.makeCurrent()) {
            log.info("Process: " + Span.current().getSpanContext().toString());
            childSpan.setAttribute("origin", "Confluent");
        } finally {
            childSpan.end();
        }
    }
    */

    public static void main( String[] args ) throws Exception {

        Properties consumerProps = configureProperties();

        final KafkaTelemetry kt = KafkaTelemetry.create(otel);

        final Consumer<String, Integer> consumer = kt.wrap(new KafkaConsumer<>(consumerProps));

        final Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                consumer.wakeup();
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Span span = tracer.spanBuilder("Reading the Energy").setSpanKind(SpanKind.CONSUMER).startSpan();
        
        try (Scope scope = span.makeCurrent()) {

            consumer.subscribe(Collections.singletonList(consumerProps.getProperty("topic")));

            while (true) {

                ConsumerRecords<String, Integer> records = consumer.poll(Duration.ofSeconds(10));

                int aggregatedEnergy = 0;
                int processedRecords = 0;

                for (ConsumerRecord<String, Integer> record : records) {
                    printRecord(record);
                    aggregatedEnergy += record.value();
                    processedRecords++;
                    if (processedRecords % 10 == 0) {
                        printAggregation(aggregatedEnergy);
                        saveAggregationToFile(aggregatedEnergy);
                        aggregatedEnergy = 0;
                    }
                }
            }

        } catch (WakeupException e) {
            log.info("Wake up exception!");

        } catch (Exception e) {
            log.error("Unexpected exception", e);

        } finally {
            span.end();
            log.info("The consumer is now gracefully closed.");
            if (consumer != null)
                consumer.close();
        }
    }
}
