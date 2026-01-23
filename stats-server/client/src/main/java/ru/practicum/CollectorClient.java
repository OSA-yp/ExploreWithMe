package ru.practicum;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;

import java.time.Instant;

@Slf4j
@Component
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub collectorStub;

    public void collectUserAction(long userId, long eventId, ActionTypeProto actionType, Instant timestamp) {
        try {
            Timestamp protoTimestamp = Timestamp.newBuilder()
                    .setSeconds(timestamp.getEpochSecond())
                    .setNanos(timestamp.getNano())
                    .build();

            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(protoTimestamp)
                    .build();

            collectorStub.collectUserAction(request);
            log.debug("User action collected: userId={}, eventId={}, actionType={}", userId, eventId, actionType);
        } catch (StatusRuntimeException e) {
            log.error("Error collecting user action: userId={}, eventId={}, actionType={}", userId, eventId, actionType, e);
            // Не пробрасываем исключение, чтобы не ломать основной функционал
        } catch (Exception e) {
            log.error("Unexpected error collecting user action", e);
            // Не пробрасываем исключение, чтобы не ломать основной функционал
        }
    }
}
