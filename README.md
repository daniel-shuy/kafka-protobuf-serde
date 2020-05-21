# kafka-protobuf-serde

| Branch  | Travis CI                                                                                                                                            | CodeFactor                                                                                                                                                                                                 | Codacy                                                                                                                                                                                                                                                                                     | Better Code Hub                                                                                                                       | Coverall                                                                                                                                                                                       |
| ------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Master  | [![Build Status](https://travis-ci.org/daniel-shuy/kafka-protobuf-serde.svg?branch=master)](https://travis-ci.org/daniel-shuy/kafka-protobuf-serde)  | [![CodeFactor](https://www.codefactor.io/repository/github/daniel-shuy/kafka-protobuf-serde/badge/master)](https://www.codefactor.io/repository/github/daniel-shuy/kafka-protobuf-serde/overview/master)   | [![Codacy Badge](https://api.codacy.com/project/badge/Grade/b20bbaee80b542edb96f068ff1b440c1?branch=master)](https://www.codacy.com/app/daniel-shuy/kafka-protobuf-serde?utm_source=github.com&utm_medium=referral&utm_content=daniel-shuy/kafka-protobuf-serde&utm_campaign=Badge_Grade)  | [![BCH compliance](https://bettercodehub.com/edge/badge/daniel-shuy/kafka-protobuf-serde?branch=master)](https://bettercodehub.com/)  | [![Coverage Status](https://coveralls.io/repos/github/daniel-shuy/kafka-protobuf-serde/badge.svg?branch=master)](https://coveralls.io/github/daniel-shuy/kafka-protobuf-serde?branch=master)   |
| Develop | [![Build Status](https://travis-ci.org/daniel-shuy/kafka-protobuf-serde.svg?branch=develop)](https://travis-ci.org/daniel-shuy/kafka-protobuf-serde) | [![CodeFactor](https://www.codefactor.io/repository/github/daniel-shuy/kafka-protobuf-serde/badge/develop)](https://www.codefactor.io/repository/github/daniel-shuy/kafka-protobuf-serde/overview/develop) | [![Codacy Badge](https://api.codacy.com/project/badge/Grade/b20bbaee80b542edb96f068ff1b440c1?branch=develop)](https://www.codacy.com/app/daniel-shuy/kafka-protobuf-serde?utm_source=github.com&utm_medium=referral&utm_content=daniel-shuy/kafka-protobuf-serde&utm_campaign=Badge_Grade) | [![BCH compliance](https://bettercodehub.com/edge/badge/daniel-shuy/kafka-protobuf-serde?branch=develop)](https://bettercodehub.com/) | [![Coverage Status](https://coveralls.io/repos/github/daniel-shuy/kafka-protobuf-serde/badge.svg?branch=develop)](https://coveralls.io/github/daniel-shuy/kafka-protobuf-serde?branch=develop) |

Serializer/Deserializer for Kafka to serialize/deserialize Protocol Buffers
messages

## Requirements

| Dependency | Version |
| ---------- | ------- |
| Kafka      | 2.X.X   |
| Protobuf   | 3.X.X   |
| Java       | 8+      |

## Usage

Add the following to your Maven dependency list:

```xml
<dependency>
    <groupId>com.github.daniel-shuy</groupId>
    <artifactId>kafka-protobuf-serde</artifactId>
    <version>2.2.0</version>
</dependency>
```

Override the `protobuf.version` property with the version of Protobuf you wish
to use (**WARNING:** do not directly override the `protobuf-java` dependency
version):

```xml
<properties>
    <protobuf.version>3.12.1</protobuf.version>
</properties>
```

Optionally, you may also override the `kafka-clients` dependency version with
the version of Kafka you wish to use:

```xml
<properties>
    <kafka.version>2.3.1</kafka.version>
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
    new KafkaProtobufDeserializer<>(MyValue.parser()));

consumer.subscribe(Collections.singleton("topic"));
ConsumerRecords<String, MyValue> records = consumer.poll(Duration.ofMillis(100));

records.forEach(record -> {
    String key = record.key();
    MyValue value = record.value();

    // ...
});
```

### Kafka Streams

```java
Serde<String> stringSerde = Serdes.String();
Serde<MyValue> myValueSerde = new KafkaProtobufSerde<>(MyValue.parser());

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
            new KafkaProtobufDeserializer<>(MyValue.parser()));
    }
}

public class Listener {
    @KafkaListener(id = "foo", topics = "annotated1")
    public void listen1(String foo) {
        // ...
    }
}
```
