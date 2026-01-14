package ru.practicum.explore.server.request.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.explore.server.request.enums.RequestStatus;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventRequestStatusUpdateRequest {
    @NotNull
    private List<Long> requestIds;

    @NotNull
    private RequestStatus status;
}