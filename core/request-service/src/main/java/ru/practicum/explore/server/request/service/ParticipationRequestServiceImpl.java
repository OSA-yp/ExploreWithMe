package ru.practicum.explore.server.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.explore.server.event.client.EventInternalClient;
import ru.practicum.explore.server.event.dto.InternalEventDto;
import ru.practicum.explore.server.event.enums.EventState;
import ru.practicum.explore.server.exception.AppException;
import ru.practicum.explore.server.exception.ConflictException;
import ru.practicum.explore.server.exception.NotFoundException;
import ru.practicum.explore.server.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explore.server.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.explore.server.request.dto.ParticipationRequestDto;
import ru.practicum.explore.server.request.enums.RequestStatus;
import ru.practicum.explore.server.request.mapper.ParticipationRequestMapper;
import ru.practicum.explore.server.request.model.ParticipationRequest;
import ru.practicum.explore.server.request.repository.ParticipationRequestRepository;
import ru.practicum.explore.server.users.client.UserInternalClient;
import ru.practicum.CollectorClient;
import ru.practicum.ewm.stats.proto.ActionTypeProto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository participationRequestRepository;
    private final EventInternalClient eventInternalClient;
    private final UserInternalClient userInternalClient;
    private final CollectorClient collectorClient;

    @Override
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        // проверка существования пользователя
        try {
            userInternalClient.getUser(userId);
        } catch (Exception e) {
            throw new AppException("Сервис пользователей недоступен", HttpStatus.SERVICE_UNAVAILABLE);
        }

        InternalEventDto event = getEvent(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя подать заявку на участие в неопубликованном событии.");
        }
        if (Objects.equals(event.getInitiatorId(), userId)) {
            throw new ConflictException("Инициатор не может подать заявку на участие в своём событии.");
        }
        if (participationRequestRepository.existsByRequesterAndEvent(userId, eventId)) {
            throw new ConflictException("Запрос на участие уже существует.");
        }

        long confirmed = participationRequestRepository.countByEventAndStatus(eventId, RequestStatus.CONFIRMED);
        int participantLimit = safeInt(event.getParticipantLimit(), 0);
        if (participantLimit > 0 && confirmed >= participantLimit) {
            throw new ConflictException("Достигнут лимит заявок на участие.");
        }

        RequestStatus status;
        boolean moderation = Boolean.TRUE.equals(event.getRequestModeration());
        if (participantLimit == 0 || !moderation) {
            status = RequestStatus.CONFIRMED;
        } else {
            status = RequestStatus.PENDING;
        }

        ParticipationRequest toSave = ParticipationRequest.builder()
                .event(eventId)
                .requester(userId)
                .created(LocalDateTime.now())
                .status(status)
                .build();

        ParticipationRequest saved = participationRequestRepository.save(toSave);
        
        // Отправка регистрации в Collector
        try {
            collectorClient.collectUserAction(userId, eventId, ActionTypeProto.ACTION_REGISTER, Instant.now());
        } catch (Exception e) {
            log.warn("Не удалось отправить регистрацию в Collector: userId={}, eventId={}", userId, eventId, e);
            // Не ломаем основной функционал при ошибке
        }
        
        return ParticipationRequestMapper.toDto(saved);
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest request) {
        InternalEventDto event = getEvent(eventId);

        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new ConflictException("Только инициатор может управлять заявками на участие.");
        }

        List<ParticipationRequest> requests = participationRequestRepository.findAllById(request.getRequestIds());
        if (requests.isEmpty()) {
            throw new NotFoundException("Заявки не найдены.");
        }

        for (ParticipationRequest participationRequest : requests) {
            if (participationRequest.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Изменять статус можно только у заявок в ожидании.");
            }
        }

        int participantLimit = safeInt(event.getParticipantLimit(), 0);
        long confirmed = participationRequestRepository.countByEventAndStatus(eventId, RequestStatus.CONFIRMED);
        if (participantLimit > 0 && confirmed >= participantLimit) {
            throw new ConflictException("Достигнут лимит заявок на участие.");
        }

        List<ParticipationRequest> confirmedRequests = new ArrayList<>();
        List<ParticipationRequest> rejectedRequests = new ArrayList<>();

        for (ParticipationRequest participationRequest : requests) {
            if (request.getStatus() == RequestStatus.CONFIRMED) {
                if (participantLimit == 0 || confirmed < participantLimit) {
                    participationRequest.setStatus(RequestStatus.CONFIRMED);
                    confirmedRequests.add(participationRequest);
                    confirmed++;
                } else {
                    participationRequest.setStatus(RequestStatus.REJECTED);
                    rejectedRequests.add(participationRequest);
                }
            } else {
                participationRequest.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(participationRequest);
            }
        }

        participationRequestRepository.saveAll(requests);

        // если после подтверждения достигли лимита - отклоняем все остальные PENDING по событию
        if (request.getStatus() == RequestStatus.CONFIRMED && participantLimit > 0 && confirmed >= participantLimit) {
            List<ParticipationRequest> pending = participationRequestRepository.findByEventAndStatus(eventId, RequestStatus.PENDING);
            for (ParticipationRequest p : pending) {
                p.setStatus(RequestStatus.REJECTED);
            }
            participationRequestRepository.saveAll(pending);
        }

        return new EventRequestStatusUpdateResult(
                ParticipationRequestMapper.toDtoList(confirmedRequests),
                ParticipationRequestMapper.toDtoList(rejectedRequests)
        );
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        // проверка существования пользователя
        try {
            userInternalClient.getUser(userId);
        } catch (Exception e) {
            throw new AppException("Сервис пользователей недоступен", HttpStatus.SERVICE_UNAVAILABLE);
        }
        List<ParticipationRequest> requests = participationRequestRepository.findByRequester(userId);
        return ParticipationRequestMapper.toDtoList(requests);
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest participationRequest = participationRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос на участие с id=" + requestId + " не найден."));

        if (!Objects.equals(participationRequest.getRequester(), userId)) {
            throw new ConflictException("Только автор запроса может отменить свою заявку.");
        }

        participationRequest.setStatus(RequestStatus.CANCELED);
        participationRequestRepository.save(participationRequest);
        return ParticipationRequestMapper.toDto(participationRequest);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        InternalEventDto event = getEvent(eventId);

        if (!Objects.equals(event.getInitiatorId(), userId)) {
            throw new ConflictException("Только инициатор события может просматривать заявки на участие.");
        }

        List<ParticipationRequest> requests = participationRequestRepository.findByEvent(eventId);
        return ParticipationRequestMapper.toDtoList(requests);
    }

    @Override
    public Map<Long, Long> getConfirmedCounts(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        return participationRequestRepository.countConfirmedRequestsForEvents(eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }

    private InternalEventDto getEvent(Long eventId) {
        try {
            return eventInternalClient.getEvent(eventId);
        } catch (Exception e) {
            throw new NotFoundException("Событие с id=" + eventId + " не найдено.");
        }
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }
}

