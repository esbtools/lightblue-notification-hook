package org.esbtools.lightbluenotificationhook;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.redhat.lightblue.generator.Description;
import com.redhat.lightblue.generator.EntityName;
import com.redhat.lightblue.generator.Identity;
import com.redhat.lightblue.generator.Required;
import com.redhat.lightblue.generator.Transient;
import com.redhat.lightblue.generator.Version;

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
@Version(value = "0.1.0", preferImplementationVersion = false, changelog = "Initial")
public class NotificationEntity {
    public static final String ENTITY_NAME = "notification";
    public static final String ENTITY_VERSION = Version.FromAnnotation.onEntity(NotificationEntity.class);

    private String _id;
    private String entityName;
    private String entityVersion;
    private Status status;
    private Operation operation;
    private String clientRequestPrincipal;
    // TODO: Would like to use JDK8 date types, but with lightblue's included version
    // of jackson, @JsonFormat does not work.
    // See: https://github.com/lightblue-platform/lightblue-core/issues/557
    // Also, for now we have JDK7 consumers; cannot go to JDK8 yet.
    private Date clientRequestDate;
    private Date processingDate;
    private Date processedDate;
    private List<PathAndValue> entityData;
    private List<String> updatedPaths;
    private List<PathAndValue> removedEntityData;
    private List<String> removedPaths;

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

    @Description("Captured entity data from the state of the entity at the time the client " +
            "request was made (see clientRequestDate). Entity data is captured as a list of key " +
            "value pairs, where the key is the lightblue path of a field, and the value is that " +
            "field's value. Only primitive values are supported, so objects are flattened to " +
            "individual paths for each field in the object.")
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

    public String getClientRequestPrincipal() {
        return clientRequestPrincipal;
    }

    @Description("If lightblue authentication is enabled, this is the principal of the client " +
            "who made the request this notification is representing.")
    public void setClientRequestPrincipal(String clientRequestPrincipal) {
        this.clientRequestPrincipal = clientRequestPrincipal;
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = LIGHTBLUE_DATE_FORMAT)
    public Date getClientRequestDate() {
        return clientRequestDate;
    }

    @Description("The date the client request was made that this notification is representing.")
    @Required
    public void setClientRequestDate(Date clientRequestDate) {
        this.clientRequestDate = clientRequestDate;
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

    public List<String> getUpdatedPaths() {
        return updatedPaths;
    }

    @Description("Lightblue paths to fields which have changed or container elements which have " +
            "been added. If array order is significant, moved array elements' paths will also be " +
            "included.")
    public void setUpdatedPaths(List<String> paths) {
        this.updatedPaths =paths;
    }

    public List<String> getRemovedPaths() {
        return removedPaths;
    }

    @Description("Lightblue paths to fields or array elements which were removed.")
    public void setRemovedPaths(List<String> paths) {
        this.removedPaths =paths;
    }

    public List<PathAndValue> getRemovedEntityData() {
        return removedEntityData;
    }

    @Description("Entity data that was removed. Each path in removedPaths will have an entry " +
            "here with the associated data to that field. As with entityData, all entries are " +
            "paths to primitive values. Objects are flattened.")
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
                Objects.equals(clientRequestPrincipal, that.clientRequestPrincipal) &&
                Objects.equals(clientRequestDate, that.clientRequestDate) &&
                Objects.equals(processingDate, that.processingDate) &&
                Objects.equals(processedDate, that.processedDate) &&
                Objects.equals(entityData, that.entityData) &&
                Objects.equals(updatedPaths, that.updatedPaths) &&
                Objects.equals(removedEntityData, that.removedEntityData) &&
                Objects.equals(removedPaths, that.removedPaths);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, entityName, entityVersion, status, operation, clientRequestPrincipal,
                clientRequestDate, processingDate, processedDate, entityData, updatedPaths,
                removedEntityData, removedPaths);
    }

    @Override
    public String toString() {
        return "NotificationEntity{" +
                "_id='" + _id + '\'' +
                ", entityName='" + entityName + '\'' +
                ", entityVersion='" + entityVersion + '\'' +
                ", status=" + status +
                ", operation=" + operation +
                ", clientRequestPrincipal='" + clientRequestPrincipal + '\'' +
                ", clientRequestDate=" + clientRequestDate +
                ", processingDate=" + processingDate +
                ", processedDate=" + processedDate +
                ", entityData=" + entityData +
                ", updatedPaths=" + updatedPaths +
                ", removedEntityData=" + removedEntityData +
                ", removedPaths=" + removedPaths +
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

        @Description("A path as in the lightblue query and projection language. For example: " +
                "'path.to.array.0.field'. Must point to primitive fields.")
        @Required
        public void setPath(String path) {
            this.path = path;
        }

        public String getValue() {
            return this.value;
        }

        @Description("Value stored in this path as a String. Only primitive values are supported.")
        public void setValue(@Nullable String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PathAndValue pathAndValue = (PathAndValue) o;
            return Objects.equals(path, pathAndValue.path) &&
                    Objects.equals(value, pathAndValue.value);
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
