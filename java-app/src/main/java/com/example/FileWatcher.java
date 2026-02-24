package com.example;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(FileWatcher.class);

    private static final String INPUT_DIR = "/data/input";
    private static final String PROCESSED_DIR = "/data/processed";
    private static final String KAFKA_BOOTSTRAP_SERVERS = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092");
    private static final String TOPIC = System.getenv().getOrDefault("KAFKA_TOPIC", "raw_messages");

    public static void main(String[] args) {
        logger.info("Starting FileWatcher producer - input: {}, processed: {}, kafka: {}, topic: {}",
                INPUT_DIR, PROCESSED_DIR, KAFKA_BOOTSTRAP_SERVERS, TOPIC);

        new File(INPUT_DIR).mkdirs();
        new File(PROCESSED_DIR).mkdirs();

        KafkaProducer<String, String> producer = createKafkaProducer();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> processFiles(producer), 0, 5, TimeUnit.SECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down FileWatcher producer");
            producer.close();
            scheduler.shutdown();
        }));
    }

    private static KafkaProducer<String, String> createKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "file-watcher-producer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        logger.info("Kafka producer initialized successfully");
        return producer;
    }

    private static void processFiles(KafkaProducer<String, String> producer) {
        File inputDir = new File(INPUT_DIR);
        File[] files = inputDir.listFiles((dir, name) -> name.endsWith(".txt"));

        if (files == null || files.length == 0) {
            return;
        }

        for (File file : files) {
            logger.info("Processing file: {}", file.getName());
            int lineCount = 0;

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    producer.send(new ProducerRecord<>(TOPIC, line));
                    lineCount++;
                }

                logger.info("Sent {} lines from file {} to topic {}", lineCount, file.getName(), TOPIC);

                Path processedPath = Paths.get(PROCESSED_DIR, file.getName());
                if (Files.exists(processedPath)) {
                    String newFileName = file.getName().replace(".txt", "_" + System.currentTimeMillis() + ".txt");
                    processedPath = Paths.get(PROCESSED_DIR, newFileName);
                    logger.warn("File {} already exists in processed directory - renamed to {}", file.getName(), newFileName);
                }

                Files.move(file.toPath(), processedPath);
                logger.info("Successfully processed and moved file: {}", file.getName());

            } catch (IOException e) {
                logger.error("Failed to process file {}: {}", file.getName(), e.getMessage(), e);
            }
        }
    }
}