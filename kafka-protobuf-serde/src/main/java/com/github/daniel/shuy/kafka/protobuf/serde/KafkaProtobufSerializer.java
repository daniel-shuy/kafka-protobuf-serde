package com.github.daniel.shuy.kafka.protobuf.serde;

import com.google.protobuf.MessageLite;
import java.util.Map;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Serializer for Kafka to serialize Protocol Buffers messages
 *
 * @param <MessageType> Protobuf message type
 */
public class KafkaProtobufSerializer<MessageType extends MessageLite> implements Serializer<MessageType> {

    /**
     * Returns a new instance of {@link KafkaProtobufSerializer}.
     */
    public KafkaProtobufSerializer() {
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, MessageType data) {
        return data.toByteArray();
    }

    @Override
    public void close() {
    }
}
