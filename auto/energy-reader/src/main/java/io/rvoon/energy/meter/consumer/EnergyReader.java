package io.rvoon.energy.meter.consumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnergyReader {

    private static final Logger log = LoggerFactory.getLogger(EnergyReader.class);

    private static void printRecord(ConsumerRecord<String, Integer> record) {
        log.info("Received record:");
        log.info("\tTopic = " + record.topic());
        log.info("\tPartition = " + record.partition());
        log.info("\tKey = " + record.key());
        log.info("\tValue = " + record.value());
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

    public static void main( String[] args ) throws Exception {
        
        Properties consumerProps = configureProperties();
        final KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(consumerProps);

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

        try {
            
            consumer.subscribe(Collections.singletonList(consumerProps.getProperty("topic")));

            while (true) {

                ConsumerRecords<String, Integer> records = consumer.poll(Duration.ofSeconds(10));

                int aggregatedEnergy = 0;
                int processedRecords = 0;

                for (ConsumerRecord<String, Integer> record : records) {
                    printRecord(record);
                    aggregatedEnergy += record.value();
                    processedRecords++;
                    if (processedRecords % 100 == 0) {
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
            log.info("The consumer is now gracefully closed.");
            if (consumer != null)
                consumer.close();
        }
    }
}
