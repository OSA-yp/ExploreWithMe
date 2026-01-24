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
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.ByteArrayInputStream;
import java.util.Map;

@Configuration
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ConsumerFactory<Long, UserActionAvro> consumerFactory() {
        // Получаем базовые настройки из конфигурации
        Map<String, Object> configProps = kafkaProperties.buildConsumerProperties();
        
        // Явно добавляем свойства из spring.kafka.consumer.properties.*
        if (kafkaProperties.getConsumer().getProperties() != null) {
            configProps.putAll(kafkaProperties.getConsumer().getProperties());
        }
        
        // Переопределяем key-deserializer на LongDeserializer
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
            "org.apache.kafka.common.serialization.LongDeserializer");
        
        // Переопределяем только value-deserializer на кастомный Avro
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
            AvroDeserializer.class.getName());
        
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
        // Получаем базовые настройки из конфигурации
        Map<String, Object> configProps = kafkaProperties.buildProducerProperties();
        
        // Явно добавляем свойства из spring.kafka.producer.properties.*
        if (kafkaProperties.getProducer().getProperties() != null) {
            configProps.putAll(kafkaProperties.getProducer().getProperties());
        }
        
        // Переопределяем key-serializer на LongSerializer
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
            "org.apache.kafka.common.serialization.LongSerializer");
        
        // Переопределяем только value-serializer на кастомный Avro
        // ВАЖНО: использовать строку с полным именем класса, а не класс напрямую
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
            AvroSerializer.class.getName());
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    @Lazy
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
