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
            log.info("Received user action: userId={}, eventId={}, actionType={}, timestamp={}",
                    request.getUserId(), request.getEventId(), request.getActionType(),
                    request.getTimestamp());

            UserActionAvro avroMessage = converter.convertToAvro(request);
            log.info("Converted to Avro: userId={}, eventId={}, actionType={}, timestamp={}",
                    avroMessage.getUserId(), avroMessage.getEventId(), avroMessage.getActionType(),
                    avroMessage.getTimestamp());

            // Синхронная отправка - ждём пока сообщение будет записано в Kafka
            kafkaTemplate.send(TOPIC_NAME, request.getEventId(), avroMessage).get();
            log.info("Sent to Kafka successfully for eventId={}", request.getEventId());

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error processing user action", e);
            responseObserver.onError(e);
        }
    }
}
