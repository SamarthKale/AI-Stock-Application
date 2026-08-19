package com.stockpredictor.backend.common;

/**
 * Thrown for a resource that either doesn't exist or belongs to a different user — the two cases
 * are deliberately indistinguishable to the caller (mapped to 404, never 403) so a request can't
 * be used to probe whether some other user's resource ID exists.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
