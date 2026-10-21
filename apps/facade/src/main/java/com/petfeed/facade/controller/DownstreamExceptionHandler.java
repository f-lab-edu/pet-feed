package com.petfeed.facade.controller;

import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DownstreamExceptionHandler {
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<Void> handle(FeignException exception) {
        int status = exception.status();
        return ResponseEntity.status(status >= 400 && status <= 599 ? status : 502).build();
    }
}
