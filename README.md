# kafka-protobuf-serde
Serializer/Deserializer for Kafka to serialize/deserialize Protocol Buffers messages

## Requirements
| Dependency | Version |
| ------- | ------------------ |
| Kafka | 2.X.X |
| Protobuf | 3.X.X |
| Java | 7+ |

## Usage
Add the following to your Maven dependency list:
```
<dependency>
    <groupId>com.github.daniel-shuy</groupId>
    <artifactId>kafka-protobuf-serde</artifactId>
    <version>2.1.0</version>
</dependency>
```
Override the `protobuf.version` property with the version of Protobuf you wish to use (**WARNING:** do not directly override the `protobuf-java` dependency version):
```
<properties>
    <protobuf.version>3.6.1</protobuf.version>
</properties>
```
Optionally, you may also override the `kafka-client` dependency version with the version of Kafka you wish to use:
```
<properties>
    <kafka.version>2.2.0/protobuf.version>
</properties>
```

### Kafka Producer
```java
Properties props = new Properties();
// props.put(..., ...);

Producer<String, MyValue> producer = new KafkaProducer<>(props,
    new StringSerializer(),
    new KafkaProtobufSerializer<>());

producer.send(new ProducerRecord<>("topic", new MyValue()));
```

### Kafka Consumer
```java
Properties props = new Properties();
// props.put(..., ...);

Consumer<String, MyValue> consumer = new KafkaConsumer<>(props,
    new StringDeserializer(),
    new KafkaProtobufDeserializer(MyValue.parser()));

consumer.subscribe("topic");
ConsumerRecords<String, MyValue> records = consumer.poll(100);

records.forEach(record -> {
    String key = record.key();
    MyValue value = record.value();

    // ...
});
```

### Kafka Streams
```java
Serde<String> stringSerde = Serdes.String();
Serde<MyValue> myValueSerde = KafkaProtobufSerde(MyValue.parser());

Properties config = new Properties();
// config.put(..., ...);

StreamsBuilder builder = new StreamsBuilder();
KStream<String, MyValue> myValues = builder.stream("input_topic", Consumed.with(stringSerde, myValueSerde));
KStream<String, MyValue> filteredMyValues = myValues.filter((key, value) -> {
    // ...
});
filteredMyValues.to("output_topic", Produced.with(stringSerde, myValueSerde));

Topology topology = builder.build();
KafkaStreams streams = new KafkaStreams(topology, config);
streams.setUncaughtExceptionHandler((thread, throwable) -> {
    // ...
});
Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
streams.start();
```

### Spring for Apache Kafka (spring-kafka)

#### Kafka Producer
```java
@Configuration
public class KafkaConfig {
    @Bean
    public ProducerFactory<String, MyValue> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        // props.put(..., ...);

        return new DefaultKafkaProducerFactory<>(producerProps,
                new StringSerializer(),
                new KafkaProtobufSerializer<>());
    }

    @Bean
    public KafkaTemplate<String, MyValue> kafkaTemplate() {
        return new KafkaTemplate(producerFactory());
    }
}
```

#### Kafka Consumer
```java
@Configuration
@EnableKafka
public class KafkaConfig {
    @Bean
    KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, MyValue>>
            kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<Integer, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, MyValue> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        // props.put(..., ...);

        return new DefaultKafkaConsumerFactory<>(props, 
            new StringDeserializer(), 
            new KafkaProtobufDeserializer(MyValue.parser()));
    }
}

public class Listener {
    @KafkaListener(id = "foo", topics = "annotated1")
    public void listen1(String foo) {
        // ...
    }
}
```
