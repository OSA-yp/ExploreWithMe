package ru.practicum.ewm.analyzer.service;

import ru.practicum.ewm.analyzer.model.EventSimilarity;
import ru.practicum.ewm.analyzer.model.UserEventInteraction;

import java.util.List;
import java.util.Map;

public interface RecommendationService {
    List<RecommendedEvent> getRecommendationsForUser(long userId, int maxResults);
    List<RecommendedEvent> getSimilarEvents(long eventId, long userId, int maxResults);
    Map<Long, Double> getInteractionsCount(List<Long> eventIds);
    boolean hasUserInteractedWithEvent(long userId, long eventId);
    
    record RecommendedEvent(long eventId, double score) {}
}
