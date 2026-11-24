package com.movento.userservice.exception;

/** A viewer-profile business rule was violated (max profiles, last profile, duplicate name). */
public class ProfileRuleException extends RuntimeException {
    public ProfileRuleException(String message) {
        super(message);
    }
}
