package com.github.daniel.shuy.kafka.protobuf.serde;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

@RunWith(SpringRunner.class)
@EmbeddedKafka(controlledShutdown = true, topics = KafkaProtobufSerializerTest.TOPIC)
@DirtiesContext
public class KafkaProtobufSerializerTest {

    static final String TOPIC = "topic";

    private static final String KAFKA_CONSUMER_GROUP_ID = "test.serializer";

    @Configuration
    static class ContextConfiguration {

        @Autowired
        private EmbeddedKafkaBroker embeddedKafka;

        @Bean
        BlockingQueue<ConsumerRecord<byte[], byte[]>> blockingQueue() {
            return new LinkedBlockingQueue<>();
        }

        @Bean
        MessageListenerContainer messageListenerContainer() {
            Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                    KAFKA_CONSUMER_GROUP_ID, Boolean.FALSE.toString(), embeddedKafka);

            ConsumerFactory<byte[], byte[]> consumerFactory = new DefaultKafkaConsumerFactory<>(
                    consumerProps,
                    new ByteArrayDeserializer(), 
                    new ByteArrayDeserializer());

            BlockingQueue<ConsumerRecord<byte[], byte[]>> records = blockingQueue();

            ContainerProperties containerProps = new ContainerProperties(TOPIC);
            containerProps.setMessageListener((MessageListener<byte[], byte[]>) records::add);

            return new KafkaMessageListenerContainer<>(
                    consumerFactory,
                    containerProps);
        }
    }

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private BlockingQueue<ConsumerRecord<byte[], byte[]>> records;

    @Autowired
    private MessageListenerContainer container;

    @Before
    public void before() {
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
    }

    @After
    public void after() {
        container.stop();
    }

    private <MessageType extends MessageLite> void serialize(
            MessageType input, Parser<MessageType> parser) throws InvalidProtocolBufferException {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);

        Serializer<MessageType> serializer = new KafkaProtobufSerializer<>();

        try (Producer<MessageType, MessageType> producer = new KafkaProducer<>(
                producerProps, 
                serializer, 
                serializer)) {
            Future<RecordMetadata> future = producer.send(new ProducerRecord<>(TOPIC, input, input));
            try {
                future.get();
            } catch (InterruptedException e) {
                return;
            } catch (ExecutionException e) {
                throw new KafkaException("Error sending message to Kafka.", e.getCause());
            }
        }

        ConsumerRecord<byte[], byte[]> consumerRecord;
        try {
            consumerRecord = records.take();
        } catch (InterruptedException e) {
            return;
        }

        byte[] outputKeyData = consumerRecord.key();
        MessageType outputKey = parser.parseFrom(outputKeyData);
        Assert.assertEquals(outputKey, input);

        byte[] outputValueData = consumerRecord.value();
        MessageType outputValue = parser.parseFrom(outputValueData);
        Assert.assertEquals(outputValue, input);
    }

    @Test(timeout = 10000)
    public void serializeProto2() throws InvalidProtocolBufferException {
        Proto2Message message = Proto2Message.newBuilder()
                .setStr("Hello World")
                .setBoolean(true)
                .setInt(Long.MIN_VALUE)
                .setDbl(Double.MIN_VALUE)
                .build();
        serialize(message, Proto2Message.parser());
    }

    @Test(timeout = 10000)
    public void serializeProto3() throws InvalidProtocolBufferException {
        Proto3Message message = Proto3Message.newBuilder()
                .setStr("Goodbye World")
                .setBoolean(false)
                .setInt(Long.MAX_VALUE)
                .setDbl(Double.MAX_VALUE)
                .build();
        serialize(message, Proto3Message.parser());
    }
}
