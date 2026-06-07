package itx.examples.kafka.service;

import itx.examples.kafka.DataMapper;
import itx.examples.kafka.dto.ServiceRequest;
import itx.examples.kafka.dto.ServiceResponse;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static itx.examples.kafka.KafkaConstants.*;

public class ProcessingServiceBackend implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessingServiceBackend.class);

    private final String serviceId;
    private final KafkaProducer<String, Bytes> producer;
    private final Consumer<String, Bytes> consumer;
    private final DataMapper dataMapper;

    private boolean running;

    public ProcessingServiceBackend(String serviceId, String brokers) {
        this.serviceId = serviceId;
        var producerSettings = new Properties();
        producerSettings.put("bootstrap.servers", brokers);
        producerSettings.put("client.id", "client-id");
        producerSettings.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerSettings.put("value.serializer", "org.apache.kafka.common.serialization.BytesSerializer");
        this.producer = new KafkaProducer<>(producerSettings);

        var consumerSettings = new Properties();
        consumerSettings.put("bootstrap.servers", brokers);
        consumerSettings.put("client.id", "client-id");
        consumerSettings.put("group.id", "consumer-group");
        consumerSettings.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerSettings.put("value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer");

        this.consumer = new KafkaConsumer<>(consumerSettings);
        this.dataMapper = new DataMapper();
        this.running = false;
    }

    public void start() {
        this.consumer.subscribe(List.of(TOPIC_SERVICE_REQUESTS));
        LOG.info("Waiting for requests {} ...", serviceId);
        this.running = true;
        while (running) {
            ConsumerRecords<String, Bytes> records = consumer.poll(Duration.ofMillis(10));
            if (!records.isEmpty()) {
                for (var record : records) {
                    try {
                        var request = dataMapper.deserialize(record.value(), ServiceRequest.class);
                        LOG.info("Received Request: {}:{}:{}", record.key(), request.clientId(), request.taskId());
                        var response = new ServiceResponse(request.taskId(), request.clientId(), request.data(), "response:" + request.data());
                        var bytes = dataMapper.serialize(response);
                        var recordReply = new ProducerRecord<>(TOPIC_SERVICE_RESPONSES, response.taskId(), bytes);
                        producer.send(recordReply);
                        LOG.info("Response has been send !");
                    } catch (IOException e) {
                        LOG.error("Exception: ", e);
                    }
                }
            }
        }
        LOG.info("done {}.", serviceId);
    }

    @Override
    public void close() throws IOException {
        LOG.info("closing {}.", serviceId);
        this.running = false;
        this.producer.close();
        this.consumer.close();
    }

    public void shutdown() {
        try {
            close();
        } catch (IOException e) {
            LOG.error("Exception: ", e);
        }
    }

}
