package ru.practicum.ewm.analyzer.config;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import org.apache.avro.io.DecoderFactory;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<Long, UserActionAvro> userActionConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // group-id, auto-offset-reset и key-deserializer берутся из конфигурации (spring.kafka.consumer.*)
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserActionAvroDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, UserActionAvro> userActionKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<Long, UserActionAvro> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userActionConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<Long, EventSimilarityAvro> eventSimilarityConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        // group-id, auto-offset-reset и key-deserializer берутся из конфигурации (spring.kafka.consumer.*)
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "analyzer-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventSimilarityAvroDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, EventSimilarityAvro> eventSimilarityKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<Long, EventSimilarityAvro> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eventSimilarityConsumerFactory());
        return factory;
    }

    public static class UserActionAvroDeserializer implements Deserializer<UserActionAvro> {
        @Override
        public UserActionAvro deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }
            try {
                ByteArrayInputStream in = new ByteArrayInputStream(data);
                DatumReader<UserActionAvro> reader = new SpecificDatumReader<>(UserActionAvro.class);
                BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(in, null);
                return reader.read(null, decoder);
            } catch (Exception e) {
                throw new RuntimeException("Error deserializing Avro message", e);
            }
        }
    }

    public static class EventSimilarityAvroDeserializer implements Deserializer<EventSimilarityAvro> {
        @Override
        public EventSimilarityAvro deserialize(String topic, byte[] data) {
            if (data == null) {
                return null;
            }
            try {
                ByteArrayInputStream in = new ByteArrayInputStream(data);
                DatumReader<EventSimilarityAvro> reader = new SpecificDatumReader<>(EventSimilarityAvro.class);
                BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(in, null);
                return reader.read(null, decoder);
            } catch (Exception e) {
                throw new RuntimeException("Error deserializing Avro message", e);
            }
        }
    }
}
