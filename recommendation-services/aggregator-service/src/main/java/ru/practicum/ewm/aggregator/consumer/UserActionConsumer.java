package ru.practicum.ewm.aggregator.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.aggregator.service.ActionWeightService;
import ru.practicum.ewm.aggregator.service.SimilarityService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private static final String USER_ACTIONS_TOPIC = "stats.user-actions.v1";
    private static final String SIMILARITY_TOPIC = "stats.events-similarity.v1";

    private final SimilarityService similarityService;
    private final ActionWeightService actionWeightService;
    private final KafkaTemplate<Long, EventSimilarityAvro> kafkaTemplate;

    @KafkaListener(topics = USER_ACTIONS_TOPIC, groupId = "aggregator-group")
    public void consume(UserActionAvro userAction) {
        try {
            log.debug("Received user action: userId={}, eventId={}, actionType={}",
                    userAction.getUserId(), userAction.getEventId(), userAction.getActionType());

            double actionWeight = actionWeightService.getWeight(userAction.getActionType());
            similarityService.updateWeights(
                    userAction.getUserId(),
                    userAction.getEventId(),
                    actionWeight
            );

            // Вычислить сходство для всех пар с этим мероприятием
            long eventId = userAction.getEventId();
            Set<Long> allEventIds = similarityService.getAllEventIds();

            for (Long otherEventId : allEventIds) {
                if (otherEventId.equals(eventId)) {
                    continue;
                }

                double similarity = similarityService.calculateSimilarity(eventId, otherEventId);
                
                if (similarity > 0.0) {
                    // Упорядочить идентификаторы
                    long eventA = Math.min(eventId, otherEventId);
                    long eventB = Math.max(eventId, otherEventId);

                    EventSimilarityAvro similarityMessage = EventSimilarityAvro.newBuilder()
                            .setEventA(eventA)
                            .setEventB(eventB)
                            .setScore(similarity)
                            .setTimestamp(Instant.now())
                            .build();

                    kafkaTemplate.send(SIMILARITY_TOPIC, eventA, similarityMessage);
                    log.debug("Sent similarity: eventA={}, eventB={}, score={}", eventA, eventB, similarity);
                }
            }
        } catch (Exception e) {
            log.error("Error processing user action", e);
        }
    }
}
