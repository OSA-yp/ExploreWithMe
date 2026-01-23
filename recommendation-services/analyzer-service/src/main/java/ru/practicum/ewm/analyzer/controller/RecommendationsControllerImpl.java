package ru.practicum.ewm.analyzer.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.*;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsControllerImpl extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(
            UserPredictionsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.debug("GetRecommendationsForUser: userId={}, maxResults={}",
                    request.getUserId(), request.getMaxResults());

            var recommendations = recommendationService.getRecommendationsForUser(
                    request.getUserId(),
                    request.getMaxResults()
            );

            for (var rec : recommendations) {
                RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                        .setEventId(rec.eventId())
                        .setScore(rec.score())
                        .build();
                responseObserver.onNext(proto);
            }

            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting recommendations for user", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSimilarEvents(
            SimilarEventsRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.debug("GetSimilarEvents: eventId={}, userId={}, maxResults={}",
                    request.getEventId(), request.getUserId(), request.getMaxResults());

            var similarEvents = recommendationService.getSimilarEvents(
                    request.getEventId(),
                    request.getUserId(),
                    request.getMaxResults()
            );

            for (var event : similarEvents) {
                RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                        .setEventId(event.eventId())
                        .setScore(event.score())
                        .build();
                responseObserver.onNext(proto);
            }

            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting similar events", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(
            InteractionsCountRequestProto request,
            StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.debug("GetInteractionsCount: eventIds={}", request.getEventIdList());

            var counts = recommendationService.getInteractionsCount(request.getEventIdList());

            for (var entry : counts.entrySet()) {
                RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build();
                responseObserver.onNext(proto);
            }

            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting interactions count", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void hasUserInteractedWithEvent(
            UserEventInteractionRequestProto request,
            io.grpc.stub.StreamObserver<com.google.protobuf.BoolValue> responseObserver) {
        try {
            log.debug("HasUserInteractedWithEvent: userId={}, eventId={}",
                    request.getUserId(), request.getEventId());

            boolean hasInteracted = recommendationService.hasUserInteractedWithEvent(
                    request.getUserId(),
                    request.getEventId()
            );

            com.google.protobuf.BoolValue response = com.google.protobuf.BoolValue.newBuilder()
                    .setValue(hasInteracted)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error checking user event interaction", e);
            responseObserver.onError(e);
        }
    }
}
