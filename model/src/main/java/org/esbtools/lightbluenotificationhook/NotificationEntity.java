package org.esbtools.lightbluenotificationhook;

import io.github.alechenninger.lightblue.Description;
import io.github.alechenninger.lightblue.Identity;
import io.github.alechenninger.lightblue.MinItems;
import io.github.alechenninger.lightblue.Required;
import io.github.alechenninger.lightblue.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    public static final String ENTITY_VERSION = "0.0.1";

    private String _id;
    private String entityName;
    private String entityVersion;
    private List<PathAndValue> entityIdentity = new ArrayList<>();
    private List<PathAndValue> entityIncludedFields = new ArrayList<>();
    private Status status;
    private Operation operation;
    /**
     * This should probably be who triggered the notification. We only do certain things if ESB
     * itself wasn't the thing that updated the data.
     */
    private String eventSource;
    private Instant occurrenceDate;

    public enum Operation {
        INSERT, UPDATE, SYNC
    }

    public String get_id() {
        return _id;
    }

    @Identity
    public NotificationEntity set_id( String _id) {
        this._id = _id;
        return this;
    }

    public String getEntityName() {
        return entityName;
    }

    @Required
    public NotificationEntity setEntityName(String entityName) {
        this.entityName = entityName;
        return this;
    }

    public String getEntityVersion() {
        return entityVersion;
    }

    @Required
    public NotificationEntity setEntityVersion(String entityVersion) {
        this.entityVersion = entityVersion;
        return this;
    }

    public List<PathAndValue> getEntityIdentity() {
        return entityIdentity;
    }

    @MinItems(1)
    public NotificationEntity setEntityIdentity(List<PathAndValue> entityIdentity) {
        this.entityIdentity = entityIdentity;
        return this;
    }

    public List<PathAndValue> getEntityIncludedFields() {
        return entityIncludedFields;
    }

    public NotificationEntity setEntityIncludedFields(List<PathAndValue> entityIncludedFields) {
        this.entityIncludedFields = entityIncludedFields;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    @Required
    public NotificationEntity setStatus(Status status) {
        this.status = status;
        return this;
    }

    public Operation getOperation() {
        return operation;
    }

    @Required
    public NotificationEntity setOperation(Operation operation) {
        this.operation = operation;
        return this;
    }

    public String getEventSource() {
        return eventSource;
    }

    @Required
    public NotificationEntity setEventSource(String eventSource) {
        this.eventSource = eventSource;
        return this;
    }

    public Instant getOccurrenceDate() {
        return occurrenceDate;
    }

    @Required
    public NotificationEntity setOccurrenceDate(Instant occurrenceDate) {
        this.occurrenceDate = occurrenceDate;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationEntity that = (NotificationEntity) o;
        return Objects.equals(_id, that._id) &&
                Objects.equals(entityName, that.entityName) &&
                Objects.equals(entityVersion, that.entityVersion) &&
                Objects.equals(entityIdentity, that.entityIdentity) &&
                Objects.equals(entityIncludedFields, that.entityIncludedFields) &&
                status == that.status &&
                operation == that.operation &&
                Objects.equals(eventSource, that.eventSource) &&
                Objects.equals(occurrenceDate, that.occurrenceDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, entityName, entityVersion, entityIdentity, entityIncludedFields, status, operation, eventSource, occurrenceDate);
    }

    @Override
    public String toString() {
        return "NotificationEntity{" +
                "_id='" + _id + '\'' +
                ", entityName='" + entityName + '\'' +
                ", entityVersion='" + entityVersion + '\'' +
                ", entityIdentity=" + entityIdentity +
                ", normalizedEntityIdentity=" + entityIncludedFields +
                ", status=" + status +
                ", operation=" + operation +
                ", eventSource='" + eventSource + '\'' +
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

        @Required
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
            return "Identity{" +
                    "path='" + path + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
