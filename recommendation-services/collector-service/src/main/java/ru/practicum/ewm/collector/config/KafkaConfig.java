package ru.practicum.ewm.collector.config;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<Long, UserActionAvro> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AvroSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
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
