package io.rvoon.energy.meter.producer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.KafkaTelemetry;

public class EnergyGenerator {

    private static final Logger log = LoggerFactory.getLogger(EnergyGenerator.class);

    static final OpenTelemetry otel = GlobalOpenTelemetry.get();
    static final Tracer tracer = otel.getTracer("io.rvoon.energy.meter", "1.0.0");
    static final int MAXCOUNT = 10;
    static final Random random = new Random();

    private static void printRecord(ProducerRecord<String, Integer> record) {

        Span span = tracer.spanBuilder("Print Energy Record").startSpan();
        try (Scope scope = span.makeCurrent()) {
            log.info("Sent record:");
            log.info("\tTopic = " + record.topic());
            log.info("\tPartition = " + record.partition());
            log.info("\tKey = " + record.key());
            log.info("\tValue = " + record.value());
        } finally {
            span.end();
        }
    }

    private static Properties configureProperties() throws IOException {
        final Properties envProps = new Properties();
        envProps.load(EnergyGenerator.class.getResourceAsStream("/producer.properties"));
        return envProps;
    }
    
    private static void sampleSpan() {
        
        Span childSpan = tracer.spanBuilder("Process Something Important").startSpan();
        try (Scope scope = childSpan.makeCurrent()) {
            log.info("Process: " + Span.current().getSpanContext().toString());
            childSpan.setAttribute("origin", "Confluent");
        } finally {
            childSpan.end();
        }
    }

    public static void main(String[] args) throws Exception {

        Span span = tracer.spanBuilder("Publish Energy").setSpanKind(SpanKind.PRODUCER).startSpan();
        
        Properties producerProps = configureProperties();
        
        final List<String> headers = new ArrayList<>(Arrays.asList("payload"));
        final KafkaTelemetry kt = KafkaTelemetry.builder(otel).setCapturedHeaders(headers).build();

        final Producer<String, Integer> producer = kt.wrap(new KafkaProducer<>(producerProps));

        try (Scope scope = span.makeCurrent()) {

            span.setAttribute("industry", "F&B");
            span.setAttribute("product", "Pre-paid");
            span.setAttribute("location", "Makati");
            
            for (int i = 0; i < MAXCOUNT; i++) {
                String turbine = "turbine-" + random.nextInt(10);
                int energyProduction = random.nextInt(1000);
                ProducerRecord<String, Integer> record = new ProducerRecord<>(producerProps.getProperty("topic"), turbine, energyProduction);
                record.headers().add("payload", String.valueOf(energyProduction).getBytes());
                producer.send(record);
                printRecord(record);
            }
            
            sampleSpan();
        
        } catch (Exception e) {
            
            log.error("Unexpected exception", e);

        } finally {

            log.info("The producer is now gracefully closed.");
            if (producer != null) 
                producer.close();

            span.end();
        }
    }
}
