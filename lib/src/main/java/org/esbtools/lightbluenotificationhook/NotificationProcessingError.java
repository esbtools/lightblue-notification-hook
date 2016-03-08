package org.esbtools.lightbluenotificationhook;

public class NotificationProcessingError extends RuntimeException {
    public NotificationProcessingError(Exception cause) {
        super(cause);
    }
}
