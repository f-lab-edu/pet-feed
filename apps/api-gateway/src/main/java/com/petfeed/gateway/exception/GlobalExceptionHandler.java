package com.petfeed.gateway.exception;

import com.petfeed.domain.exception.DownstreamException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DownstreamException.class)
    public ResponseEntity<byte[]> handleDownstream(DownstreamException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
    }
}
