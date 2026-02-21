package com.fleetflow.exception;

public class HistoryRangeTooLargeException extends RuntimeException {
    public HistoryRangeTooLargeException(String message) {
        super(message);
    }
}
