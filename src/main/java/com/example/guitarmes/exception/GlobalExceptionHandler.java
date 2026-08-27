package com.example.guitarmes.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.guitarmes.dto.ErrorResponse;

@RestControllerAdvice(
        basePackages =
                "com.example.guitarmes.controller.api")
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse>
            handleBusinessException(
                    BusinessException exception) {

        ErrorResponse response =
                new ErrorResponse(
                        exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse>
            handleNotFoundException(
                    NotFoundException exception) {

        ErrorResponse response =
                new ErrorResponse(
                        exception.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }
}