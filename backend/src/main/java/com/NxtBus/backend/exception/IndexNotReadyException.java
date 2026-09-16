package com.nxtbus.backend.exception;

/** Thrown when a routing request arrives before the RAPTOR index has been
 *  built (cold start) or after a build failure left no snapshot published. */
public class IndexNotReadyException extends RuntimeException {
    public IndexNotReadyException() {
        super("RAPTOR index is not ready yet — try again shortly");
    }
}
