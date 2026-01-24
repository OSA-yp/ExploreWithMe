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

import ru.practicum.ewm.aggregator.service.SimilarityService.Pair;

import java.time.Instant;
import java.util.List;

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
            long eventId = userAction.getEventId();
            
            similarityService.updateWeights(
                    userAction.getUserId(),
                    eventId,
                    actionWeight
            );

            // Получить только измененные пары схожести
            List<Pair<Long, Long>> changedPairs = similarityService.getChangedSimilarities(eventId);

            for (Pair<Long, Long> pair : changedPairs) {
                long eventA = pair.getFirst();
                long eventB = pair.getSecond();
                
                // Фильтровать: отправлять только пары, где текущий eventId является eventA
                // (после упорядочивания eventA = min(eventId, otherEventId))
                if (eventA != eventId) {
                    continue; // Пропустить пары, где eventId является eventB
                }
                
                double similarity = similarityService.calculateSimilarity(eventA, eventB);
                
                if (similarity > 0.0) {
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
            
            // Очистить список измененных пар после отправки
            similarityService.clearChangedSimilarities(eventId);
        } catch (Exception e) {
            log.error("Error processing user action", e);
        }
    }
}
