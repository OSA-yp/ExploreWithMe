package ru.practicum.explore.server.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.explore.server.event.enums.EventState;
import ru.practicum.explore.server.event.model.Event;
import ru.practicum.explore.server.event.repository.EventRepository;
import ru.practicum.explore.server.exception.ConflictException;
import ru.practicum.explore.server.exception.NotFoundException;
import ru.practicum.explore.server.exception.ValidationException;
import ru.practicum.explore.server.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.explore.server.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.explore.server.request.dto.ParticipationRequestDto;
import ru.practicum.explore.server.request.enums.RequestStatus;
import ru.practicum.explore.server.request.mapper.ParticipationRequestMapper;
import ru.practicum.explore.server.request.model.ParticipationRequest;
import ru.practicum.explore.server.request.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {

    private final ParticipationRequestRepository participationRequestRepository;
    private final EventRepository eventRepository;

    @Override
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя подать заявку на участие в неопубликованном событии.");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Инициатор не может подать заявку на участие в своём событии.");
        }

        if (participationRequestRepository.existsByRequesterAndEvent(userId, eventId)) {
            throw new ConflictException("Запрос на участие уже существует.");
        }

        long confirmedRequests = participationRequestRepository.countByEventAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнуто максимальное количество участников для события c id: " + eventId + ".");
        }

        ParticipationRequestDto requestDto = new ParticipationRequestDto();
        requestDto.setCreated(LocalDateTime.now());
        requestDto.setEvent(event.getId());
        requestDto.setRequester(userId);

        RequestStatus status = Boolean.TRUE.equals(event.getRequestModeration()) ? RequestStatus.PENDING : RequestStatus.CONFIRMED;
        if (event.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
        }
        requestDto.setStatus(status);

        ParticipationRequest savedRequest = participationRequestRepository.save(
                ParticipationRequestMapper.toEntity(requestDto)
        );
        return ParticipationRequestMapper.toDto(savedRequest);
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));

        if (!event.getInitiator().getId().equals(userId)) {
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

        long confirmedRequests = participationRequestRepository.countByEventAndStatus(eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит заявок на участие.");
        }

        List<ParticipationRequest> confirmed = new ArrayList<>();
        List<ParticipationRequest> rejected = new ArrayList<>();

        for (ParticipationRequest participationRequest : requests) {
            if (request.getStatus() == RequestStatus.CONFIRMED) {
                if (confirmedRequests < event.getParticipantLimit() || event.getParticipantLimit() == 0) {
                    participationRequest.setStatus(RequestStatus.CONFIRMED);
                    confirmed.add(participationRequest);
                    confirmedRequests++;
                } else {
                    participationRequest.setStatus(RequestStatus.REJECTED);
                    rejected.add(participationRequest);
                }
            } else {
                participationRequest.setStatus(RequestStatus.REJECTED);
                rejected.add(participationRequest);
            }
        }

        participationRequestRepository.saveAll(requests);

        return new EventRequestStatusUpdateResult(
                ParticipationRequestMapper.toDtoList(confirmed),
                ParticipationRequestMapper.toDtoList(rejected)
        );
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        List<ParticipationRequest> requests = participationRequestRepository.findByRequester(userId);
        return ParticipationRequestMapper.toDtoList(requests);
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest participationRequest = participationRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос на участие с id=" + requestId + " не найден."));

        if (!participationRequest.getRequester().equals(userId)) {
            throw new ConflictException("Только автор запроса может отменить свою заявку.");
        }

        participationRequest.setStatus(RequestStatus.CANCELED);
        participationRequestRepository.save(participationRequest);

        return ParticipationRequestMapper.toDto(participationRequest);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено."));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ValidationException("Только инициатор события может просматривать заявки на участие.");
        }

        List<ParticipationRequest> requests = participationRequestRepository.findByEvent(eventId);
        return ParticipationRequestMapper.toDtoList(requests);
    }
}