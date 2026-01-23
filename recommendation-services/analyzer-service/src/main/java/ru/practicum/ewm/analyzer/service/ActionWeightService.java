package ru.practicum.ewm.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

@Slf4j
@Service
public class ActionWeightService {

    private static final double WEIGHT_VIEW = 1.0;
    private static final double WEIGHT_REGISTER = 2.0;
    private static final double WEIGHT_LIKE = 3.0;

    public double getWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> WEIGHT_VIEW;
            case REGISTER -> WEIGHT_REGISTER;
            case LIKE -> WEIGHT_LIKE;
        };
    }
}
