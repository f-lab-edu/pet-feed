package com.petfeed.domain.exception;

import lombok.Getter;

@Getter
public class DownstreamException extends RuntimeException {

    private final int statusCode;
    private final byte[] body;

    public DownstreamException(int statusCode, byte[] body) {
        super("Downstream service responded with status: " + statusCode);
        this.statusCode = statusCode;
        this.body = body;
    }

}
