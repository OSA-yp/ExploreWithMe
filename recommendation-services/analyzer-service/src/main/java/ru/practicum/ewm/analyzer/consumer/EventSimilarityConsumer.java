package ru.practicum.ewm.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.analyzer.model.EventSimilarity;
import ru.practicum.ewm.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityConsumer {

    private static final String SIMILARITY_TOPIC = "stats.events-similarity.v1";

    private final EventSimilarityRepository repository;

    @KafkaListener(topics = SIMILARITY_TOPIC, groupId = "analyzer-group", containerFactory = "eventSimilarityKafkaListenerContainerFactory")
    @Transactional
    public void consume(EventSimilarityAvro similarity) {
        try {
            log.debug("Received similarity: eventA={}, eventB={}, score={}",
                    similarity.getEventA(), similarity.getEventB(), similarity.getScore());

            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    similarity.getTimestamp(),
                    java.time.ZoneId.systemDefault()
            );

            EventSimilarity eventSimilarity = EventSimilarity.builder()
                    .eventA(similarity.getEventA())
                    .eventB(similarity.getEventB())
                    .score(similarity.getScore())
                    .timestamp(timestamp)
                    .build();

            // UPSERT: сохранить или обновить существующую запись
            repository.save(eventSimilarity);
            log.debug("Saved similarity: eventA={}, eventB={}, score={}",
                    similarity.getEventA(), similarity.getEventB(), similarity.getScore());
        } catch (Exception e) {
            log.error("Error processing event similarity", e);
        }
    }
}
