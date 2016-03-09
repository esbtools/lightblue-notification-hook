package org.esbtools.lightbluenotificationhook;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.alechenninger.lightblue.Description;
import io.github.alechenninger.lightblue.EntityName;
import io.github.alechenninger.lightblue.Identity;
import io.github.alechenninger.lightblue.MinItems;
import io.github.alechenninger.lightblue.Required;
import io.github.alechenninger.lightblue.Transient;
import io.github.alechenninger.lightblue.Version;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Serialization-friendly "data object" for an entity in the notification collection.
 *
 * <p>Notifications include just the very immediate, basic information known at the time of a CRUD
 * operation on an integrated entity on integrated fields.
 */
@EntityName(NotificationEntity.ENTITY_NAME)
@Version(value = NotificationEntity.ENTITY_VERSION, changelog = "Initial")
public class NotificationEntity {
    public static final String ENTITY_NAME = "notification";
    public static final String ENTITY_VERSION = "0.0.1-SNAPSHOT";

    private String _id;
    private String entityName;
    private String entityVersion;
    private Status status;
    private Operation operation;
    private String triggeredByUser;
    // TODO: Would like to use JDK8 date types, but with lightblue's included version
    // of jackson, @JsonFormat does not work.
    // See: https://github.com/lightblue-platform/lightblue-core/issues/557
    private Date occurrenceDate;
    private Date processingDate;
    private Date processedDate;
    private List<PathAndValue> entityData;
    private List<String> changedPaths;
    private List<PathAndValue> removedEntityData;
    private List<String> removedElements;

    private static final String LIGHTBLUE_DATE_FORMAT = "yyyyMMdd\'T\'HH:mm:ss.SSSZ";

    public enum Operation {
        insert, update
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

    public List<PathAndValue> getEntityData() {
        return entityData;
    }

    public void setEntityData(List<PathAndValue> data) {
        this.entityData=data;
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

    public void setTriggeredByUser(String triggeredByUser) {
        this.triggeredByUser = triggeredByUser;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = LIGHTBLUE_DATE_FORMAT)
    public Date getOccurrenceDate() {
        return occurrenceDate;
    }

    @Required
    public void setOccurrenceDate(Date occurrenceDate) {
        this.occurrenceDate = occurrenceDate;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = LIGHTBLUE_DATE_FORMAT)
    public Date getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate(Date processingDate) {
        this.processingDate = processingDate;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = LIGHTBLUE_DATE_FORMAT)
    public Date getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(Date processedDate) {
        this.processedDate = processedDate;
    }

    public List<String> getChangedPaths() {
        return changedPaths;
    }

    public void setChangedPaths(List<String> paths) {
        this.changedPaths=paths;
    }

    public List<String> getRemovedElements() {
        return removedElements;
    }

    public void setRemovedElements(List<String> paths) {
        this.removedElements=paths;
    }

    public List<PathAndValue> getRemovedEntityData() {
        return removedEntityData;
    }

    public void setRemovedEntityData(List<PathAndValue> l) {
        this.removedEntityData=l;
    }

    @Transient
    public boolean hasEntityDataForField(String fieldPath) {
        for (PathAndValue pathAndValue : entityData) {
            if (Objects.equals(fieldPath, pathAndValue.getPath())) {
                return true;
            }
        }
        return false;
    }
    
    @Transient
    @Nullable
    public String getEntityDataForField(String fieldPath) {
        for (PathAndValue pathAndValue : entityData) {
            if (Objects.equals(fieldPath, pathAndValue.getPath())) {
                return pathAndValue.getValue();
            }
        }
        throw new NoSuchElementException(fieldPath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationEntity that = (NotificationEntity) o;
        return Objects.equals(_id, that._id) &&
                Objects.equals(entityName, that.entityName) &&
                Objects.equals(entityVersion, that.entityVersion) &&
                status == that.status &&
                operation == that.operation &&
                Objects.equals(triggeredByUser, that.triggeredByUser) &&
                Objects.equals(occurrenceDate, that.occurrenceDate) &&
                Objects.equals(processingDate, that.processingDate) &&
                Objects.equals(processedDate, that.processedDate) &&
                Objects.equals(entityData, that.entityData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, entityName, entityVersion, status, operation, triggeredByUser,
                occurrenceDate, processingDate, processedDate, entityData);
    }

    @Override
    public String toString() {
        return "NotificationEntity{" +
                "_id='" + _id + '\'' +
                ", entityName='" + entityName + '\'' +
                ", entityVersion='" + entityVersion + '\'' +
                ", status=" + status +
                ", operation=" + operation +
                ", triggeredByUser='" + triggeredByUser + '\'' +
                ", occurrenceDate=" + occurrenceDate +
                ", processingDate=" + processingDate +
                ", processedDate=" + processedDate +
                ", entityData=" + entityData +
                '}';
    }

    public enum Status {
        @Description("Persist fresh notifications as 'unprocessed'. " +
                "Unprocessed notifications are available to be processed.")
        unprocessed,
        @Description("Processing notifications should only be worked on in one thread at a time.")
        processing,
        @Description("Final state for a notification. Should not be reprocessed.")
        processed,
        @Description("Something went wrong when trying to determine what document events this " +
                "notification should produce.")
        failed
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
