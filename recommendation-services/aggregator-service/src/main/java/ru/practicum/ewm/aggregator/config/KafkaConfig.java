package ru.practicum.ewm.aggregator.config;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<Long, UserActionAvro> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "aggregator-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, AvroDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, UserActionAvro> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<Long, UserActionAvro> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public ProducerFactory<Long, EventSimilarityAvro> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<Long, EventSimilarityAvro> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    public static class AvroDeserializer implements Deserializer<UserActionAvro> {
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

    public static class AvroSerializer implements Serializer<EventSimilarityAvro> {
        @Override
        public byte[] serialize(String topic, EventSimilarityAvro data) {
            if (data == null) {
                return null;
            }
            try {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                DatumWriter<EventSimilarityAvro> writer = new SpecificDatumWriter<>(EventSimilarityAvro.class);
                BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
                writer.write(data, encoder);
                encoder.flush();
                return out.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException("Error serializing Avro message", e);
            }
        }
    }
}
