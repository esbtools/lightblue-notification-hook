package org.esbtools.lightbluenotificationhook;

import io.github.alechenninger.lightblue.Description;
import io.github.alechenninger.lightblue.Identity;
import io.github.alechenninger.lightblue.MinItems;
import io.github.alechenninger.lightblue.Required;
import io.github.alechenninger.lightblue.Transient;
import io.github.alechenninger.lightblue.Version;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Serialization-friendly "data object" for an entity in the notification collection.
 *
 * <p>Notifications include just the very immediate, basic information known at the time of a CRUD
 * operation on an integrated entity on integrated fields.
 */
@Version(value = NotificationEntity.ENTITY_VERSION, changelog = "Initial")
public class NotificationEntity {
    public static final String ENTITY_NAME = "notification";
    public static final String ENTITY_VERSION = "0.0.1-SNAPSHOT";

    private String _id;
    private String entityName;
    private String entityVersion;
    private List<PathAndValue> entityData;
    private Status status;
    private Operation operation;
    private String triggeredByUser;
    private Instant occurrenceDate;

    public enum Operation {
        INSERT, UPDATE
    }

    public String get_id() {
        return _id;
    }

    @Identity
    public void set_id(String _id) {
        this._id = _id;
    }

    public String getEntityName() {
        return entityName;
    }

    @Required
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getEntityVersion() {
        return entityVersion;
    }

    @Required
    public void setEntityVersion(String entityVersion) {
        this.entityVersion = entityVersion;
    }

    @Transient
    public String getEntityDataForField(String fieldPath) {
        for (PathAndValue pathAndValue : entityData) {
            if (Objects.equals(fieldPath, pathAndValue.getPath())) {
                return pathAndValue.getValue();
            }
        }
        throw new NoSuchElementException(fieldPath);
    }

    public List<PathAndValue> getEntityData() {
        return entityData;
    }

    @Required
    @MinItems(1)
    public void setEntityData(List<PathAndValue> entityData) {
        this.entityData = entityData;
    }

    public Status getStatus() {
        return status;
    }

    @Required
    public void setStatus(Status status) {
        this.status = status;
    }

    public Operation getOperation() {
        return operation;
    }

    @Required
    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public String getTriggeredByUser() {
        return triggeredByUser;
    }

    @Required
    public void setTriggeredByUser(String triggeredByUser) {
        this.triggeredByUser = triggeredByUser;
    }

    public Instant getOccurrenceDate() {
        return occurrenceDate;
    }

    @Required
    public void setOccurrenceDate(Instant occurrenceDate) {
        this.occurrenceDate = occurrenceDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationEntity that = (NotificationEntity) o;
        return Objects.equals(_id, that._id) &&
                Objects.equals(entityName, that.entityName) &&
                Objects.equals(entityVersion, that.entityVersion) &&
                Objects.equals(entityData, that.entityData) &&
                status == that.status &&
                operation == that.operation &&
                Objects.equals(triggeredByUser, that.triggeredByUser) &&
                Objects.equals(occurrenceDate, that.occurrenceDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, entityName, entityVersion, entityData, status, operation, triggeredByUser, occurrenceDate);
    }

    @Override
    public String toString() {
        return "NotificationEntity{" +
                "_id='" + _id + '\'' +
                ", entityName='" + entityName + '\'' +
                ", entityVersion='" + entityVersion + '\'' +
                ", entityData=" + entityData +
                ", status=" + status +
                ", operation=" + operation +
                ", triggeredByUser='" + triggeredByUser + '\'' +
                ", occurrenceDate=" + occurrenceDate +
                '}';
    }

    public enum Status {
        @Description("Persist fresh notifications as 'new'. " +
                "New notifications are available to be processed.")
        NEW,
        @Description("Processing notifications should only be worked on in one thread at a time.")
        PROCESSING,
        @Description("Final state for a notification. Should not be reprocessed.")
        PROCESSED,
        FAILED
    }

    public static class PathAndValue {
        private String path;
        private String value;

        public PathAndValue() {
        }

        public PathAndValue(String path, String value) {
            this.path = path;
            this.value = value;
        }

        public String getPath() {
            return this.path;
        }

        @Required
        public void setPath(String path) {
            this.path = path;
        }

        public String getValue() {
            return this.value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PathAndValue identityValue = (PathAndValue) o;
            return Objects.equals(path, identityValue.path) &&
                    Objects.equals(value, identityValue.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path, value);
        }

        @Override
        public String toString() {
            return "PathAndValue{" +
                    "path='" + path + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
