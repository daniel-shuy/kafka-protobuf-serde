package com.github.daniel.shuy.kafka.protobuf.serde;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import java.util.Map;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Deserializer for Kafka to deserialize Protocol Buffers messages
 *
 * @param <T> Protobuf message type
 */
public class KafkaProtobufDeserializer<T extends MessageLite> implements Deserializer<T> {

    private final Parser<T> parser;

    /**
     * Returns a new instance of {@link KafkaProtobufDeserializer}.
     *
     * @param parser The Protobuf {@link Parser}.
     */
    public KafkaProtobufDeserializer(Parser<T> parser) {
        this.parser = parser;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            return parser.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new SerializationException("Error deserializing from Protobuf message", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
    }
}
