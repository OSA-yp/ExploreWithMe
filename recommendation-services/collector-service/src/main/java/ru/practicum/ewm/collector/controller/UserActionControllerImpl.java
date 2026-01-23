package ru.practicum.ewm.collector.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.collector.service.UserActionConverter;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import com.google.protobuf.Empty;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionControllerImpl extends UserActionControllerGrpc.UserActionControllerImplBase {

    private static final String TOPIC_NAME = "stats.user-actions.v1";

    private final KafkaTemplate<Long, UserActionAvro> kafkaTemplate;
    private final UserActionConverter converter;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.debug("Received user action: userId={}, eventId={}, actionType={}",
                    request.getUserId(), request.getEventId(), request.getActionType());

            UserActionAvro avroMessage = converter.convertToAvro(request);
            kafkaTemplate.send(TOPIC_NAME, request.getEventId(), avroMessage);

            log.debug("User action sent to Kafka: eventId={}", request.getEventId());

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing user action", e);
            responseObserver.onError(e);
        }
    }
}
