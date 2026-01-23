package ru.practicum.ewm.aggregator.service;

import java.util.Map;

public interface SimilarityService {
    void updateWeights(long userId, long eventId, double actionWeight);
    double calculateSimilarity(long eventA, long eventB);
    void putMinWeightsSum(long eventA, long eventB, double sum);
    double getMinWeightsSum(long eventA, long eventB);
    java.util.Set<Long> getAllEventIds();
}
