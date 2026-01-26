package ru.practicum;

import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.*;
import com.google.protobuf.BoolValue;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub analyzerStub;

    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        try {
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerStub.getRecommendationsForUser(request);
            return iteratorToStream(iterator);
        } catch (StatusRuntimeException e) {
            log.error("Error getting recommendations for user: userId={}, maxResults={}", userId, maxResults, e);
            return Stream.empty();
        } catch (Exception e) {
            log.error("Unexpected error getting recommendations for user", e);
            return Stream.empty();
        }
    }

    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        try {
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerStub.getSimilarEvents(request);
            return iteratorToStream(iterator);
        } catch (StatusRuntimeException e) {
            log.error("Error getting similar events: eventId={}, userId={}, maxResults={}", eventId, userId, maxResults, e);
            return Stream.empty();
        } catch (Exception e) {
            log.error("Unexpected error getting similar events", e);
            return Stream.empty();
        }
    }

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        try {
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();

            Iterator<RecommendedEventProto> iterator = analyzerStub.getInteractionsCount(request);
            return iteratorToStream(iterator);
        } catch (StatusRuntimeException e) {
            log.error("Error getting interactions count: eventIds={}", eventIds, e);
            return Stream.empty();
        } catch (Exception e) {
            log.error("Unexpected error getting interactions count", e);
            return Stream.empty();
        }
    }

    public boolean hasUserInteractedWithEvent(long userId, long eventId) {
        try {
            UserEventInteractionRequestProto request = UserEventInteractionRequestProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .build();

            com.google.protobuf.BoolValue response = analyzerStub.hasUserInteractedWithEvent(request);
            return response.getValue();
        } catch (StatusRuntimeException e) {
            log.error("Error checking user event interaction: userId={}, eventId={}", userId, eventId, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error checking user event interaction", e);
            return false;
        }
    }

    private <T> Stream<T> iteratorToStream(Iterator<T> iterator) {
        Iterable<T> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
