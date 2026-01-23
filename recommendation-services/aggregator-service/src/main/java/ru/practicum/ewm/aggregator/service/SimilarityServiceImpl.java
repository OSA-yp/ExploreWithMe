package ru.practicum.ewm.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class SimilarityServiceImpl implements SimilarityService {

    // eventId -> userId -> maxWeight
    private final Map<Long, Map<Long, Double>> eventUserWeights = new HashMap<>();
    
    // eventId -> totalWeight
    private final Map<Long, Double> eventTotalWeights = new HashMap<>();
    
    // eventA -> eventB -> S_min
    private final Map<Long, Map<Long, Double>> minWeightsSums = new HashMap<>();

    @Override
    public void updateWeights(long userId, long eventId, double actionWeight) {
        // Получить текущий максимальный вес для пары (userId, eventId)
        double oldWeight = eventUserWeights
                .getOrDefault(eventId, new HashMap<>())
                .getOrDefault(userId, 0.0);

        // Максимальный вес не изменился - пересчет не нужен
        if (actionWeight <= oldWeight) {
            return;
        }

        double newWeight = actionWeight;
        double weightDiff = newWeight - oldWeight;

        // Обновить матрицу весов
        eventUserWeights.computeIfAbsent(eventId, k -> new HashMap<>())
                .put(userId, newWeight);

        // Обновить общую сумму весов для мероприятия
        eventTotalWeights.merge(eventId, weightDiff, Double::sum);

        // Пересчитать сходство для всех пар с этим мероприятием
        recalculateSimilarities(eventId, weightDiff);
    }

    private void recalculateSimilarities(long eventId, double weightDiff) {
        Set<Long> allEventIds = eventUserWeights.keySet();
        Map<Long, Double> userWeightsForEvent = eventUserWeights.get(eventId);
        
        if (userWeightsForEvent == null) {
            return;
        }
        
        for (Long otherEventId : allEventIds) {
            if (otherEventId.equals(eventId)) {
                continue;
            }

            // Пересчитать S_min для пары (eventId, otherEventId)
            recalculateMinWeightsSum(eventId, otherEventId, userWeightsForEvent);
        }
    }

    private void recalculateMinWeightsSum(long eventA, long eventB, Map<Long, Double> userWeightsA) {
        Map<Long, Double> userWeightsB = eventUserWeights.getOrDefault(eventB, new HashMap<>());

        // Пересчитать сумму минимальных весов для всех пользователей, взаимодействовавших с обоими мероприятиями
        double totalMinSum = 0.0;
        for (Long userId : userWeightsA.keySet()) {
            if (userWeightsB.containsKey(userId)) {
                double weightA = userWeightsA.get(userId);
                double weightB = userWeightsB.get(userId);
                totalMinSum += Math.min(weightA, weightB);
            }
        }

        putMinWeightsSum(eventA, eventB, totalMinSum);
    }

    @Override
    public double calculateSimilarity(long eventA, long eventB) {
        double sMin = getMinWeightsSum(eventA, eventB);
        double sA = eventTotalWeights.getOrDefault(eventA, 0.0);
        double sB = eventTotalWeights.getOrDefault(eventB, 0.0);

        if (sA == 0.0 || sB == 0.0) {
            return 0.0;
        }

        return sMin / Math.sqrt(sA * sB);
    }

    @Override
    public void putMinWeightsSum(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        minWeightsSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    @Override
    public double getMinWeightsSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);
        return minWeightsSums
                .computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }

    public Set<Long> getAllEventIds() {
        return eventUserWeights.keySet();
    }
}
