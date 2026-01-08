package com.auth.exception;

/**
 * This class is the refresh token error.
 * It is used when refresh token is not valid.
 */
public class RefreshTokenException extends RuntimeException {

    /**
     * This is the constructor with message.
     *
     * @param message the error text
     */
    public RefreshTokenException(String message) {
        super(message);
    }
}
