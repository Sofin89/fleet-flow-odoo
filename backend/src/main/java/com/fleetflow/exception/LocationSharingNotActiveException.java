package com.fleetflow.exception;

public class LocationSharingNotActiveException extends RuntimeException {
    public LocationSharingNotActiveException(String message) {
        super(message);
    }
}
