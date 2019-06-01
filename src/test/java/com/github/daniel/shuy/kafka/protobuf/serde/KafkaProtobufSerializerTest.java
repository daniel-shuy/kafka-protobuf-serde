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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

@RunWith(SpringRunner.class)
@EmbeddedKafka(controlledShutdown = true)
public class KafkaProtobufSerializerTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    private <MessageType extends MessageLite> void serialize(
            MessageType input, Parser<MessageType> parser) throws InvalidProtocolBufferException {
        // generate a random UUID to create a unique topic and consumer group id for each test
        String uuid = UUID.randomUUID().toString();
        String topic = "topic-" + uuid;

        embeddedKafka.addTopics(topic);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                uuid, Boolean.TRUE.toString(), embeddedKafka);
        ConsumerFactory<byte[], byte[]> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new ByteArrayDeserializer(),
                new ByteArrayDeserializer());

        BlockingQueue<ConsumerRecord<byte[], byte[]>> records = new LinkedBlockingQueue<>();
        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setMessageListener((MessageListener<byte[], byte[]>) records::add);

        MessageListenerContainer container = new KafkaMessageListenerContainer<>(
                consumerFactory,
                containerProps);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());

        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        Serializer<MessageType> serializer = new KafkaProtobufSerializer<>();
        try (Producer<MessageType, MessageType> producer = new KafkaProducer<>(
                producerProps,
                serializer,
                serializer)) {
            Future<RecordMetadata> future = producer.send(new ProducerRecord<>(topic, input, input));
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
