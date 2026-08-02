package com.nevgiu.hrai.evaluation;

import org.springframework.http.HttpStatus;

public class EvaluationException extends RuntimeException {
    private final HttpStatus status;

    public EvaluationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
