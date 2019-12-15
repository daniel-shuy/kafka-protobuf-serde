package com.github.daniel.shuy.kafka.protobuf.serde;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Predicate;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.test.ConsumerRecordFactory;
import org.apache.kafka.streams.test.OutputVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
public class KafkaProtobufSerdeTest {
    @Test
    public void streamProto2() {
        Proto2Message input = Proto2Message.newBuilder()
                .setStr("Hello World")
                .setBoolean(true)
                .setInt(Long.MIN_VALUE)
                .setDbl(Double.MIN_VALUE)
                .build();
        Proto2Message output = Proto2Message.newBuilder()
                .setStr("Goodbye World")
                .setBoolean(false)
                .setInt(Long.MAX_VALUE)
                .setDbl(Double.MAX_VALUE)
                .build();
        stream(input, output, Proto2Message.parser());
    }

    @Test
    public void streamProto3() {
        Proto3Message input = Proto3Message.newBuilder()
                .setStr("Hello World")
                .setBoolean(true)
                .setInt(Long.MIN_VALUE)
                .setDbl(Double.MIN_VALUE)
                .build();
        Proto3Message output = Proto3Message.newBuilder()
                .setStr("Goodbye World")
                .setBoolean(false)
                .setInt(Long.MAX_VALUE)
                .setDbl(Double.MAX_VALUE)
                .build();
        stream(input, output, Proto3Message.parser());
    }

    private <T extends MessageLite> void stream(T input, T output, Parser<T> parser) {
        // generate a random UUID to create unique topics and application id for each test
        String uuid = UUID.randomUUID().toString();
        String topic = "topic-" + uuid;
        String inputTopic = "input_" + topic;
        String outputTopic = "output_" + topic;
        String errorTopic = "error_" + topic;

        Properties props = new Properties();
        props.setProperty(StreamsConfig.APPLICATION_ID_CONFIG, uuid);
        props.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");

        Serde<T> serde = new KafkaProtobufSerde<>(parser);

        StreamsBuilder builder = new StreamsBuilder();
        Predicate<T, T> predicate = (key, value) -> Objects.equals(key, input) && Objects.equals(value, input);
        KStream<T, T>[] streams = builder.stream(inputTopic, Consumed.with(serde, serde))
                .branch(predicate, negate(predicate));
        streams[0]
                .map((key, value) -> new KeyValue<>(output, output))
                .to(outputTopic, Produced.with(serde, serde));
        streams[1]
                .map((key, value) -> {
                    if (!Objects.equals(key, input)) {
                        return new KeyValue<>(null, "key: " + formatEqualsFailed(key, input));
                    }
                    if (!Objects.equals(value, input)) {
                        return new KeyValue<>(null, "value: " + formatEqualsFailed(value, input));
                    }
                    return null;
                })
                .to(errorTopic, Produced.valueSerde(Serdes.String()));

        Topology topology = builder.build();

        TopologyTestDriver testDriver = new TopologyTestDriver(topology, props);

        Serializer<T> serializer = serde.serializer();
        ConsumerRecordFactory<T, T> factory = new ConsumerRecordFactory<>(inputTopic, serializer, serializer);
        testDriver.pipeInput(factory.create(input, input));

        Deserializer<T> deserializer = serde.deserializer();
        ProducerRecord<Object, String> errorRecord = testDriver.readOutput(errorTopic, null, new StringDeserializer());
        if (errorRecord != null) {
            throw new AssertionError(errorRecord.value());
        }

        ProducerRecord<T, T> outputRecord = testDriver.readOutput(outputTopic, deserializer, deserializer);
        OutputVerifier.compareKeyValue(outputRecord, output, output);

        testDriver.close();
    }

    /**
     * @see java.util.function.Predicate#negate()
     * @see java.util.function.Predicate#not(java.util.function.Predicate)
     */
    private static <K, V> Predicate<K, V> negate(Predicate<K, V> predicate) {
        return (key, value) -> !predicate.test(key, value);
    }

    /**
     * See {@code Assert#format(String, Object, Object)}
     */
    private static String formatEqualsFailed(Object expected, Object actual) {
        String expectedString = String.valueOf(expected);
        String actualString = String.valueOf(actual);
        if (expectedString.equals(actualString)) {
            return "expected: " + formatClassAndValue(expected, expectedString)
                    + " but was: " + formatClassAndValue(actual, actualString);
        } else {
            return "expected:<" + expectedString + "> but was:<"
                    + actualString + ">";
        }
    }

    /**
     * See {@code Assert#formatClassAndValue(Object, String)}
     */
    private static String formatClassAndValue(Object value, String valueString) {
        String className = value == null ? "null" : value.getClass().getName();
        return className + "<" + valueString + ">";
    }
}
