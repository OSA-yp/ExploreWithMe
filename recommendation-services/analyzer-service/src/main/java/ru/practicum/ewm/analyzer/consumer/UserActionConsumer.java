package ru.practicum.ewm.analyzer.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.analyzer.model.UserEventInteraction;
import ru.practicum.ewm.analyzer.repository.UserEventInteractionRepository;
import ru.practicum.ewm.analyzer.service.ActionWeightService;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {

    private static final String USER_ACTIONS_TOPIC = "stats.user-actions.v1";

    private final UserEventInteractionRepository repository;
    private final ActionWeightService actionWeightService;

    @KafkaListener(topics = USER_ACTIONS_TOPIC, groupId = "analyzer-group", containerFactory = "userActionKafkaListenerContainerFactory")
    @Transactional
    public void consume(UserActionAvro userAction) {
        try {
            log.debug("Received user action: userId={}, eventId={}, actionType={}",
                    userAction.getUserId(), userAction.getEventId(), userAction.getActionType());

            double actionWeight = actionWeightService.getWeight(userAction.getActionType());
            LocalDateTime timestamp = LocalDateTime.ofInstant(
                    userAction.getTimestamp(),
                    java.time.ZoneId.systemDefault()
            );

            repository.findById(new ru.practicum.ewm.analyzer.model.UserEventInteractionId(
                    userAction.getUserId(),
                    userAction.getEventId()
            )).ifPresentOrElse(
                    existing -> {
                        // Обновить максимальный вес, если новый больше
                        if (actionWeight > existing.getMaxWeight()) {
                            existing.setMaxWeight(actionWeight);
                            existing.setLastActionTimestamp(timestamp);
                            repository.save(existing);
                            log.debug("Updated interaction: userId={}, eventId={}, maxWeight={}",
                                    userAction.getUserId(), userAction.getEventId(), actionWeight);
                        } else if (timestamp.isAfter(existing.getLastActionTimestamp())) {
                            // Обновить timestamp даже если вес не изменился
                            existing.setLastActionTimestamp(timestamp);
                            repository.save(existing);
                        }
                    },
                    () -> {
                        // Создать новую запись
                        UserEventInteraction interaction = UserEventInteraction.builder()
                                .userId(userAction.getUserId())
                                .eventId(userAction.getEventId())
                                .maxWeight(actionWeight)
                                .lastActionTimestamp(timestamp)
                                .build();
                        repository.save(interaction);
                        log.debug("Created interaction: userId={}, eventId={}, maxWeight={}",
                                userAction.getUserId(), userAction.getEventId(), actionWeight);
                    }
            );
        } catch (Exception e) {
            log.error("Error processing user action", e);
        }
    }
}
