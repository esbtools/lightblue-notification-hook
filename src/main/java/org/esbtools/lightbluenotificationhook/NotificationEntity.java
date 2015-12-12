package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.EntityVersion;

import javax.annotation.Nullable;
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
public class NotificationEntity {
    private static final EntityVersion VERSION = new EntityVersion("notification", "0.0.1");

    @Nullable
    private String _id;
    private String entityName;
    private String entityVersion;
    private List<PathAndValue> entityIdentity = new ArrayList<>();
    private List<PathAndValue> entityIncludedFields = new ArrayList<>();
    private EventStatus status;
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

    public EntityVersion entityVersion() {
        return VERSION;
    }

    public String get_id() {
        return _id;
    }

    public NotificationEntity set_id(String _id) {
        this._id = _id;
        return this;
    }

    public String getEntityName() {
        return entityName;
    }

    public NotificationEntity setEntityName(String entityName) {
        this.entityName = entityName;
        return this;
    }

    public String getEntityVersion() {
        return entityVersion;
    }

    public NotificationEntity setEntityVersion(String entityVersion) {
        this.entityVersion = entityVersion;
        return this;
    }

    public List<PathAndValue> getEntityIdentity() {
        return entityIdentity;
    }

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

    public EventStatus getStatus() {
        return status;
    }

    public NotificationEntity setStatus(EventStatus status) {
        this.status = status;
        return this;
    }

    public Operation getOperation() {
        return operation;
    }

    public NotificationEntity setOperation(Operation operation) {
        this.operation = operation;
        return this;
    }

    public String getEventSource() {
        return eventSource;
    }

    public NotificationEntity setEventSource(String eventSource) {
        this.eventSource = eventSource;
        return this;
    }

    public Instant getOccurrenceDate() {
        return occurrenceDate;
    }

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
}
