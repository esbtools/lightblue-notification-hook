package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.DataError;
import com.redhat.lightblue.EntityVersion;
import com.redhat.lightblue.Response;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.config.LightblueFactoryAware;
import com.redhat.lightblue.crud.InsertionRequest;
import com.redhat.lightblue.eval.Projector;
import com.redhat.lightblue.hooks.CRUDHook;
import com.redhat.lightblue.hooks.HookDoc;
import com.redhat.lightblue.mediator.Mediator;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.Field;
import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonDoc;
import com.redhat.lightblue.util.JsonNodeCursor;
import com.redhat.lightblue.util.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationHook implements CRUDHook, LightblueFactoryAware {
    private final String name;
    private final JsonNodeFactory jsonNodeFactory;
    private final ObjectMapper objectMapper;

    private LightblueFactory lightblueFactory;

    public NotificationHook(String name) {
        this(name, new ObjectMapper(), JsonNodeFactory.withExactBigDecimals(true));
    }

    public NotificationHook(String name, ObjectMapper objectMapper,
            JsonNodeFactory jsonNodeFactory) {
        this.name = name;
        this.jsonNodeFactory = jsonNodeFactory;
        this.objectMapper = objectMapper;
    }

    @Override
    public void setLightblueFactory(LightblueFactory lightblueFactory) {
        this.lightblueFactory = lightblueFactory;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void processHook(EntityMetadata entityMetadata, HookConfiguration hookConfiguration,
            List<HookDoc> hookDocs) {
        if (!(hookConfiguration instanceof NotificationHookConfiguration)) {
            throw new IllegalArgumentException("Expected instance of " +
                    "NotificationHookConfiguration but got: " + hookConfiguration);
        }

        NotificationHookConfiguration config = (NotificationHookConfiguration) hookConfiguration;
        Mediator mediator = tryGetMediator();

        Projector watchProjector = Projector.getInstance(config.watchProjection(), entityMetadata);
        Projector includeProjector = Projector.getInstance(config.includeProjection(),
                entityMetadata);

        for (HookDoc hookDoc : hookDocs) {
            HookResult result = processSingleHookDoc(watchProjector,
                    includeProjector, hookDoc, mediator);

            // TODO: batch
            if (result.hasErrorsOrDataErrors()) {
                throw new NotificationInsertErrorsException(result.entity, result.errors,
                        result.dataErrors);
            }
        }
    }

    private HookResult processSingleHookDoc(Projector watchProjector, Projector includeProjector,
            HookDoc hookDoc, Mediator mediator) {
        JsonDoc postDoc = hookDoc.getPostDoc();
        JsonDoc preDoc = hookDoc.getPreDoc();

        if (postDoc == null) {
            return HookResult.aborted();
        }

        if (watchedFieldsHaveChanged(preDoc, postDoc, watchProjector)) {
            NotificationEntity notification =
                    makeNotificationEntityWithIncludedFields(hookDoc, includeProjector);

            EntityVersion notificationVersion = new EntityVersion(
                    NotificationEntity.ENTITY_NAME,
                    NotificationEntity.ENTITY_VERSION);

            InsertionRequest newNotification = new InsertionRequest();

            newNotification.setEntityVersion(notificationVersion);
            newNotification.setEntityData(objectMapper.valueToTree(notification));

            Response response = mediator.insert(newNotification);

            return new HookResult(notification, response.getErrors(), response.getDataErrors());
        }

        return HookResult.aborted();
    }

    private boolean watchedFieldsHaveChanged(JsonDoc preDoc, JsonDoc postDoc,
            Projector watchProjector) {
        JsonDoc watchedPostDoc = watchProjector.project(postDoc, jsonNodeFactory);
        JsonDoc watchedPreDoc = preDoc == null
                ? new JsonDoc(jsonNodeFactory.nullNode())
                : watchProjector.project(preDoc, jsonNodeFactory);

        return !watchedPostDoc.getRoot().equals(watchedPreDoc.getRoot());
    }

    private NotificationEntity makeNotificationEntityWithIncludedFields(HookDoc hookDoc,
            Projector includeProjector) {
        EntityMetadata metadata = hookDoc.getEntityMetadata();
        JsonDoc postDoc = hookDoc.getPostDoc();

        List<NotificationEntity.PathAndValue> data = new ArrayList<>();

        for (Field identityField : metadata.getEntitySchema().getIdentityFields()) {
            Path identityPath = identityField.getFullPath();

            String pathString = identityPath.toString();
            String valueString = postDoc.get(identityPath).asText();

            data.add(new NotificationEntity.PathAndValue(pathString, valueString));
        }

        JsonDoc includedDoc = includeProjector.project(postDoc, jsonNodeFactory);
        JsonNodeCursor cursor = includedDoc.cursor();
        while (cursor.next()) {
            if (cursor.parent()) {
                continue;
            }

            String pathString = cursor.getCurrentPath().toString();
            String valueString = cursor.getCurrentNode().asText();

            data.add(new NotificationEntity.PathAndValue(pathString, valueString));
        }

        NotificationEntity.Operation operation = hookDoc.getPreDoc() == null
                ? NotificationEntity.Operation.INSERT
                : NotificationEntity.Operation.UPDATE;

        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setEntityData(data);
        notificationEntity.setEntityName(metadata.getName());
        notificationEntity.setEntityVersion(metadata.getVersion().getValue());
        notificationEntity.setOperation(operation);
        notificationEntity.setTriggeredByUser(hookDoc.getWho());
        notificationEntity.setOccurrenceDate(hookDoc.getWhen().toInstant());
        notificationEntity.setStatus(NotificationEntity.Status.NEW);

        return notificationEntity;
    }

    private Mediator tryGetMediator() {
        // TODO: Could cache this result?
        try {
            return lightblueFactory.getMediator();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to get Mediator from LightblueFactory!", e);
        }
    }

    static class HookResult {
        final NotificationEntity entity;
        final List<Error> errors;
        final List<DataError> dataErrors;

        static HookResult aborted() {
            return new HookResult(null, Collections.<Error>emptyList(),
                    Collections.<DataError>emptyList());
        }

        HookResult(NotificationEntity entity, List<Error> errors, List<DataError> dataErrors) {
            this.entity = entity;
            this.errors = errors;
            this.dataErrors = dataErrors;
        }

        boolean hasErrorsOrDataErrors() {
            return !errors.isEmpty() || !dataErrors.isEmpty();
        }
    }
}
