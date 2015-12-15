package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.DataError;
import com.redhat.lightblue.util.Error;

import java.util.Collection;
import java.util.List;

public class NotificationInsertErrorsException extends RuntimeException {
    public NotificationInsertErrorsException(NotificationEntity entity, Collection<Error> errors,
            Collection<DataError> dataErrors) {
        super("Errors inserting new notification: " + entity + "\n" +
                "Data errors: " + dataErrors + "\n" +
                "Errors: " + errors);
    }
}
