package ru.practicum.ewm.aggregator.service;

import java.util.List;
import java.util.Map;

public interface SimilarityService {
    void updateWeights(long userId, long eventId, double actionWeight);
    double calculateSimilarity(long eventA, long eventB);
    void putMinWeightsSum(long eventA, long eventB, double sum);
    double getMinWeightsSum(long eventA, long eventB);
    java.util.Set<Long> getAllEventIds();
    List<Pair<Long, Long>> getChangedSimilarities(long eventId);
    void clearChangedSimilarities(long eventId);
    
    class Pair<F, S> {
        private final F first;
        private final S second;
        
        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }
        
        public F getFirst() {
            return first;
        }
        
        public S getSecond() {
            return second;
        }
    }
}
