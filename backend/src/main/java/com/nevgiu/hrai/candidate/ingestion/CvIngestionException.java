package com.nevgiu.hrai.candidate.ingestion;

import org.springframework.http.HttpStatus;

public class CvIngestionException extends RuntimeException {
    private final HttpStatus status;

    public CvIngestionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
