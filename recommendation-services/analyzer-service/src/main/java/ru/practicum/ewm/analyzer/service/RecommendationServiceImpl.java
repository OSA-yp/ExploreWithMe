package ru.practicum.ewm.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.analyzer.model.EventSimilarity;
import ru.practicum.ewm.analyzer.model.UserEventInteraction;
import ru.practicum.ewm.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.analyzer.repository.UserEventInteractionRepository;

import java.util.*;
import java.util.Comparator;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final int DEFAULT_RECENT_INTERACTIONS = 10;
    private static final int DEFAULT_K_NEIGHBORS = 5;

    private final UserEventInteractionRepository userInteractionRepository;
    private final EventSimilarityRepository similarityRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RecommendedEvent> getRecommendationsForUser(long userId, int maxResults) {
        // Этап 1: Подбор мероприятий
        List<Long> recentEventIds = getRecentInteractions(userId, DEFAULT_RECENT_INTERACTIONS);
        if (recentEventIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Найти похожие новые мероприятия
        Set<Long> candidateEventIds = new HashSet<>();
        Set<Long> interactedEventIds = new HashSet<>(
                userInteractionRepository.findByUserIdOrderByTimestampDesc(userId).stream()
                        .map(UserEventInteraction::getEventId)
                        .toList()
        );

        for (Long recentEventId : recentEventIds) {
            List<EventSimilarity> similarities = similarityRepository.findSimilarEvents(recentEventId);
            for (EventSimilarity sim : similarities) {
                long otherEventId = sim.getEventA().equals(recentEventId) ? sim.getEventB() : sim.getEventA();
                if (!interactedEventIds.contains(otherEventId)) {
                    candidateEventIds.add(otherEventId);
                }
            }
        }

        // Выбрать N самых похожих
        Map<Long, Double> candidateScores = new HashMap<>();
        for (Long candidateId : candidateEventIds) {
            double maxSimilarity = 0.0;
            for (Long recentEventId : recentEventIds) {
                double similarity = getSimilarity(recentEventId, candidateId);
                maxSimilarity = Math.max(maxSimilarity, similarity);
            }
            candidateScores.put(candidateId, maxSimilarity);
        }

        List<Long> topCandidates = candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .toList();

        // Этап 2: Вычисление оценки для каждого мероприятия
        List<RecommendedEvent> recommendations = new ArrayList<>();
        for (Long eventId : topCandidates) {
            double predictedRating = calculatePredictedRating(userId, eventId, DEFAULT_K_NEIGHBORS);
            recommendations.add(new RecommendedEvent(eventId, predictedRating));
        }

        return recommendations.stream()
                .sorted(Comparator.comparing(RecommendedEvent::score).reversed())
                .toList();
    }

    private List<Long> getRecentInteractions(long userId, int limit) {
        return userInteractionRepository.findByUserIdOrderByTimestampDesc(userId).stream()
                .limit(limit)
                .map(UserEventInteraction::getEventId)
                .toList();
    }

    private double getSimilarity(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return similarityRepository.findById(new ru.practicum.ewm.analyzer.model.EventSimilarityId(first, second))
                .map(EventSimilarity::getScore)
                .orElse(0.0);
    }

    private double calculatePredictedRating(long userId, long eventId, int k) {
        // Найти K ближайших соседей среди тех, с которыми пользователь взаимодействовал
        List<EventSimilarity> allSimilarities = similarityRepository.findSimilarEvents(eventId);
        
        // Получить все мероприятия, с которыми пользователь взаимодействовал
        Set<Long> interactedEventIds = new HashSet<>(
                userInteractionRepository.findByUserIdOrderByTimestampDesc(userId).stream()
                        .map(UserEventInteraction::getEventId)
                        .toList()
        );

        // Отфильтровать похожие мероприятия, оставив только те, с которыми пользователь взаимодействовал
        // Отсортировать по коэффициенту подобия DESC и взять первые K
        List<EventSimilarity> neighborSimilarities = allSimilarities.stream()
                .filter(sim -> {
                    long neighborId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
                    return interactedEventIds.contains(neighborId);
                })
                .sorted(Comparator.comparing(EventSimilarity::getScore).reversed())
                .limit(k)
                .toList();

        if (neighborSimilarities.isEmpty()) {
            return 0.0;
        }

        // Получить оценки (максимальные веса) для соседей
        List<Long> neighborEventIds = neighborSimilarities.stream()
                .map(sim -> sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA())
                .toList();

        List<UserEventInteraction> neighborInteractions = userInteractionRepository
                .findByUserIdAndEventIdIn(userId, neighborEventIds);

        if (neighborInteractions.isEmpty()) {
            return 0.0;
        }

        // Вычислить сумму взвешенных оценок и сумму коэффициентов подобия
        double sumWeightedScores = 0.0;
        double sumSimilarities = 0.0;

        Map<Long, Double> interactionMap = neighborInteractions.stream()
                .collect(Collectors.toMap(
                        UserEventInteraction::getEventId,
                        UserEventInteraction::getMaxWeight
                ));

        for (EventSimilarity sim : neighborSimilarities) {
            long neighborId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
            if (interactionMap.containsKey(neighborId)) {
                double similarity = sim.getScore();
                double rating = interactionMap.get(neighborId);
                sumWeightedScores += similarity * rating;
                sumSimilarities += similarity;
            }
        }

        if (sumSimilarities == 0.0) {
            return 0.0;
        }

        return sumWeightedScores / sumSimilarities;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendedEvent> getSimilarEvents(long eventId, long userId, int maxResults) {
        // Получить похожие мероприятия
        List<EventSimilarity> similarities = similarityRepository.findByEventId(eventId);

        // Получить мероприятия, с которыми пользователь взаимодействовал
        Set<Long> interactedEventIds = new HashSet<>(
                userInteractionRepository.findByUserIdOrderByTimestampDesc(userId).stream()
                        .map(UserEventInteraction::getEventId)
                        .toList()
        );

        // Исключить просмотренные мероприятия (где пользователь взаимодействовал с обоими)
        List<RecommendedEvent> result = new ArrayList<>();
        for (EventSimilarity sim : similarities) {
            long otherEventId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
            
            // Исключить, если пользователь взаимодействовал с обоими мероприятиями
            if (interactedEventIds.contains(eventId) && interactedEventIds.contains(otherEventId)) {
                continue;
            }

            result.add(new RecommendedEvent(otherEventId, sim.getScore()));
        }

        return result.stream()
                .sorted(Comparator.comparing(RecommendedEvent::score).reversed())
                .limit(maxResults)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Double> getInteractionsCount(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> results = userInteractionRepository.sumMaxWeightsByEventId(eventIds);
        Map<Long, Double> counts = new HashMap<>();
        
        for (Object[] result : results) {
            Long eventId = ((Number) result[0]).longValue();
            Double sum = ((Number) result[1]).doubleValue();
            counts.put(eventId, sum);
        }

        // Добавить нули для мероприятий без взаимодействий
        for (Long eventId : eventIds) {
            counts.putIfAbsent(eventId, 0.0);
        }

        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasUserInteractedWithEvent(long userId, long eventId) {
        return userInteractionRepository.existsByUserIdAndEventId(userId, eventId);
    }
}
