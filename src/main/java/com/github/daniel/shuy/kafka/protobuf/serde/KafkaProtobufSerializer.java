package com.github.daniel.shuy.kafka.protobuf.serde;

import com.google.protobuf.MessageLite;
import java.util.Map;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Serializer for Kafka to serialize Protocol Buffers messages
 *
 * @param <T> Protobuf message type
 */
public class KafkaProtobufSerializer<T extends MessageLite> implements Serializer<T> {
    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, T data) {
        return data.toByteArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
    }
}
