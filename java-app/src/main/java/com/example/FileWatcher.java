package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FileWatcher {
    private static final String INPUT_DIR = "/data/input";
    private static final String PROCESSED_DIR = "/data/processed";
    private static final String KAFKA_BOOTSTRAP_SERVERS = "kafka:9092";
    private static final String TOPIC = "raw_messages";

    public static void main(String[] args) {
        new File(INPUT_DIR).mkdirs();
        new File(PROCESSED_DIR).mkdirs();

        KafkaProducer<String, String> producer = createKafkaProducer();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> processFiles(producer), 0, 5, TimeUnit.SECONDS);
    }

    private static KafkaProducer<String, String> createKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    private static void processFiles(KafkaProducer<String, String> producer) {
        File inputDir = new File(INPUT_DIR);
        File[] files = inputDir.listFiles((dir, name) -> name.endsWith(".txt"));

        if (files != null) {
            for (File file : files) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        producer.send(new ProducerRecord<>(TOPIC, line));
                    }
                    // Move file to processed
                    Path processedPath = Paths.get(PROCESSED_DIR, file.getName());
                    Files.move(file.toPath(), processedPath);
                    System.out.println("Processed and moved: " + file.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
