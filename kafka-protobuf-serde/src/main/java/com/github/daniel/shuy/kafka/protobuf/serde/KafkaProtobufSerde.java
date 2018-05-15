package com.github.daniel.shuy.kafka.protobuf.serde;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import java.util.Map;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public class KafkaProtobufSerde<MessageType extends MessageLite> implements Serde<MessageType> {

    private final Serializer<MessageType> serializer;
    private final Deserializer<MessageType> deserializer;

    /**
     * Returns a new instance of {@link KafkaProtobufSerde}.
     *
     * @param parser The Protobuf {@link Parser}.
     */
    public KafkaProtobufSerde(Parser<MessageType> parser) {
        serializer = new KafkaProtobufSerializer<>();
        deserializer = new KafkaProtobufDeserializer<>(parser);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }

    @Override
    public Serializer<MessageType> serializer() {
        return serializer;
    }

    @Override
    public Deserializer<MessageType> deserializer() {
        return deserializer;
    }
}
