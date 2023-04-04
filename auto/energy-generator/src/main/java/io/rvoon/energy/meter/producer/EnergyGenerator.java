package io.rvoon.energy.meter.producer;

import java.io.IOException;
import java.util.Properties;
import java.util.Random;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnergyGenerator {

    private static final Logger log = LoggerFactory.getLogger(EnergyGenerator.class);
    
    private static final int MAXCOUNT = 10;
    private static final Random random = new Random();

    private static Properties configureProperties() throws IOException {
        final Properties envProps = new Properties();
        envProps.load(EnergyGenerator.class.getResourceAsStream("/producer.properties"));
        return envProps;
    }

    private static void printRecord(ProducerRecord<String, Integer> record) {
        log.info("Sent record:");
        log.info("\tTopic = " + record.topic());
        log.info("\tPartition = " + record.partition());
        log.info("\tKey = " + record.key());
        log.info("\tValue = " + record.value());
    }

    public static void main(String[] args) throws Exception {

        final Properties producerProps = configureProperties();
        
        final Producer<String, Integer> producer = new KafkaProducer<>(producerProps);
        
        try {
            
            for (int i = 0; i < MAXCOUNT; i++) {
                String turbine = "turbine-" + random.nextInt(10);
                int energyProduction = random.nextInt(1000);
                ProducerRecord<String, Integer> record = new ProducerRecord<>(producerProps.getProperty("topic"), turbine, energyProduction);
                producer.send(record);
                printRecord(record);
            }

        } catch (Exception e) {

            log.error("Unexpected exception", e);
            
        } finally {
            
            log.info("The producer is now gracefully closed.");
            if (producer != null) 
                producer.close();
        }
    }
}
