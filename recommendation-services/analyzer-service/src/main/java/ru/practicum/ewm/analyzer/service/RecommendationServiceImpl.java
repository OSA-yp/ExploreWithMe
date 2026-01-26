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
        // Загрузить все взаимодействия пользователя один раз
        List<UserEventInteraction> allInteractions = userInteractionRepository.findByUserIdOrderByTimestampDesc(userId);
        
        if (allInteractions.isEmpty()) {
            return Collections.emptyList();
        }

        // Этап 1: Подбор мероприятий
        // Получить N недавних мероприятий (отсортированы по дате DESC)
        List<Long> recentEventIds = allInteractions.stream()
                .limit(DEFAULT_RECENT_INTERACTIONS)
                .map(UserEventInteraction::getEventId)
                .toList();

        // Множество всех мероприятий, с которыми пользователь взаимодействовал
        Set<Long> interactedEventIds = allInteractions.stream()
                .map(UserEventInteraction::getEventId)
                .collect(Collectors.toSet());

        // Map для быстрого доступа к maxWeight по eventId
        Map<Long, Double> userRatings = allInteractions.stream()
                .collect(Collectors.toMap(
                        UserEventInteraction::getEventId,
                        UserEventInteraction::getMaxWeight
                ));

        // Найти похожие новые мероприятия (с которыми пользователь не взаимодействовал)
        Set<Long> candidateEventIds = new HashSet<>();
        for (Long recentEventId : recentEventIds) {
            List<EventSimilarity> similarities = similarityRepository.findSimilarEvents(recentEventId);
            for (EventSimilarity sim : similarities) {
                long otherEventId = sim.getEventA().equals(recentEventId) ? sim.getEventB() : sim.getEventA();
                if (!interactedEventIds.contains(otherEventId)) {
                    candidateEventIds.add(otherEventId);
                }
            }
        }

        if (candidateEventIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Выбрать N самых похожих по максимальному коэффициенту подобия
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

        // Этап 2: Вычисление предсказанной оценки для каждого мероприятия
        List<RecommendedEvent> recommendations = new ArrayList<>();
        for (Long eventId : topCandidates) {
            double predictedRating = calculatePredictedRating(eventId, interactedEventIds, userRatings, DEFAULT_K_NEIGHBORS);
            recommendations.add(new RecommendedEvent(eventId, predictedRating));
        }

        return recommendations.stream()
                .sorted(Comparator.comparing(RecommendedEvent::score).reversed())
                .toList();
    }

    private double getSimilarity(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return similarityRepository.findById(new ru.practicum.ewm.analyzer.model.EventSimilarityId(first, second))
                .map(EventSimilarity::getScore)
                .orElse(0.0);
    }

    /**
     * Вычисляет предсказанную оценку для мероприятия на основе K ближайших соседей.
     * Использует уже загруженные данные о взаимодействиях пользователя.
     */
    private double calculatePredictedRating(long eventId, Set<Long> interactedEventIds, 
                                            Map<Long, Double> userRatings, int k) {
        // Найти K ближайших соседей среди тех, с которыми пользователь взаимодействовал
        List<EventSimilarity> allSimilarities = similarityRepository.findSimilarEvents(eventId);

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

        // Вычислить сумму взвешенных оценок и сумму коэффициентов подобия
        // predicted_rating = sum(similarity * rating) / sum(similarity)
        double sumWeightedScores = 0.0;
        double sumSimilarities = 0.0;

        for (EventSimilarity sim : neighborSimilarities) {
            long neighborId = sim.getEventA().equals(eventId) ? sim.getEventB() : sim.getEventA();
            Double rating = userRatings.get(neighborId);
            if (rating != null) {
                double similarity = sim.getScore();
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
        // Получить похожие мероприятия (только с положительным коэффициентом подобия)
        List<EventSimilarity> similarities = similarityRepository.findSimilarEvents(eventId);

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
