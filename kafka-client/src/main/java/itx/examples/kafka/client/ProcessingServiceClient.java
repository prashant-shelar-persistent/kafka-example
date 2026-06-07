package itx.examples.kafka.client;

import itx.examples.kafka.SingleRecordConsumerJob;
import itx.examples.kafka.DataMapper;
import itx.examples.kafka.ProcessingException;
import itx.examples.kafka.ProcessingService;
import itx.examples.kafka.dto.ServiceRequest;
import itx.examples.kafka.dto.ServiceResponse;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static itx.examples.kafka.KafkaConstants.*;

public class ProcessingServiceClient implements ProcessingService, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessingServiceClient.class);

    private final KafkaProducer<String, Bytes> producer;
    private final Consumer<String, Bytes> consumer;
    private final ExecutorService executor;
    private final DataMapper dataMapper;

    public ProcessingServiceClient(String clientId, String brokers) {
        var producerSettings = new Properties();
        producerSettings.put("bootstrap.servers", brokers);
        producerSettings.put("client.id", clientId);
        producerSettings.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerSettings.put("value.serializer", "org.apache.kafka.common.serialization.BytesSerializer");
        this.producer = new KafkaProducer<>(producerSettings);

        var consumerSettings = new Properties();
        consumerSettings.put("bootstrap.servers", brokers);
        consumerSettings.put("client.id", "client-id");
        consumerSettings.put("group.id", "consumer-group-" + clientId);
        consumerSettings.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerSettings.put("value.deserializer", "org.apache.kafka.common.serialization.BytesDeserializer");

        this.consumer = new KafkaConsumer<>(consumerSettings);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.dataMapper = new DataMapper();
    }

    public void init() {
        this.consumer.subscribe(List.of(TOPIC_SERVICE_RESPONSES));
        LOG.info("consumer subscribed");
    }

    @Override
    public Future<ServiceResponse> process(ServiceRequest request) throws ProcessingException {
        try {
            var consumerJob = new SingleRecordConsumerJob(this.consumer, request.taskId());
            var response = executor.submit(consumerJob);
            var bytes = dataMapper.serialize(request);
            var record = new ProducerRecord<>(TOPIC_SERVICE_REQUESTS, request.taskId(), bytes);
            LOG.info("Sending request ...");
            producer.send(record);
            return response;
        } catch (IOException e) {
            throw new ProcessingException(e);
        }
    }

    @Override
    public void close() {
        this.producer.close();
        this.consumer.close();
        this.executor.shutdown();
    }

}
