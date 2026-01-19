package ru.practicum.explore.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;


@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse exceptionHandler(ValidationException e) {
        return new ErrorResponse("ValidationException", e.getMessage(), e.getStackTrace());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse notFoundExceptionHandler(NotFoundException e) {
        return new ErrorResponse("NotFoundException", e.getMessage(), e.getStackTrace());
    }

    @ExceptionHandler({ConflictException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse conflictExceptionHandler(ConflictException e) {
        return new ErrorResponse("ConflictException", e.getMessage(), e.getStackTrace());
    }

    @ExceptionHandler({ForbiddenException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse forbiddenExceptionHandler(ForbiddenException e) {
        return new ErrorResponse("ForbiddenException", e.getMessage(), e.getStackTrace());
    }

    @ExceptionHandler({AppException.class})
    public ErrorResponse appExceptionHandler(AppException e) {
        return new ErrorResponse("AppException", e.getMessage(), e.getStackTrace());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("Field: %s. Error: %s. Value: %s",
                        error.getField(),
                        error.getDefaultMessage(),
                        error.getRejectedValue()))
                .collect(Collectors.joining("; "));
        return new ErrorResponse("BAD_REQUEST", "Incorrectly made request. " + message, e.getStackTrace());
    }

}
