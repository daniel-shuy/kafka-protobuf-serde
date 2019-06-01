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
 * @param <MessageType> Protobuf message type
 */
public class KafkaProtobufDeserializer<MessageType extends MessageLite> implements Deserializer<MessageType> {

    private final Parser<MessageType> parser;

    /**
     * Returns a new instance of {@link KafkaProtobufDeserializer}.
     *
     * @param parser The Protobuf {@link Parser}.
     */
    public KafkaProtobufDeserializer(Parser<MessageType> parser) {
        this.parser = parser;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public MessageType deserialize(String topic, byte[] data) {
        try {
            return parser.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new SerializationException("Error deserializing from Protobuf message", e);
        }
    }

    @Override
    public void close() {
    }
}
