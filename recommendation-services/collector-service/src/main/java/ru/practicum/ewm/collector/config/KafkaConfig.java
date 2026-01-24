package ru.practicum.ewm.collector.config;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Configuration
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ProducerFactory<Long, UserActionAvro> producerFactory() {
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
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
            AvroSerializer.class.getName());
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    @Lazy
    public KafkaTemplate<Long, UserActionAvro> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    public static class AvroSerializer implements Serializer<UserActionAvro> {
        @Override
        public byte[] serialize(String topic, UserActionAvro data) {
            if (data == null) {
                return null;
            }
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DatumWriter<UserActionAvro> writer = new SpecificDatumWriter<>(UserActionAvro.class);
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
