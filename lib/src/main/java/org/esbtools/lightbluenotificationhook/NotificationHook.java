package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.ClientIdentification;
import com.redhat.lightblue.DataError;
import com.redhat.lightblue.EntityVersion;
import com.redhat.lightblue.Response;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.config.LightblueFactoryAware;
import com.redhat.lightblue.crud.CRUDOperation;
import com.redhat.lightblue.crud.InsertionRequest;
import com.redhat.lightblue.eval.Projector;
import com.redhat.lightblue.hooks.CRUDHook;
import com.redhat.lightblue.hooks.HookDoc;
import com.redhat.lightblue.mediator.Mediator;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.Field;
import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.query.FieldProjection;
import com.redhat.lightblue.query.Projection;
import com.redhat.lightblue.query.ProjectionList;
import com.redhat.lightblue.util.DocComparator;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonCompare;
import com.redhat.lightblue.util.JsonDoc;
import com.redhat.lightblue.util.JsonNodeCursor;
import com.redhat.lightblue.util.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.esbtools.lightbluenotificationhook.NotificationEntity.PathAndValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NotificationHook implements CRUDHook, LightblueFactoryAware {
    private final String name;
    private final JsonNodeFactory jsonNodeFactory;
    private final ObjectMapper objectMapper;

    private @Nullable LightblueFactory lightblueFactory;
    private @Nullable volatile Mediator mediator;

    private static final Logger LOGGER=LoggerFactory.getLogger(NotificationHook.class);

    private final ClientIdentification notificationHookClientId = new ClientIdentification() {
        @Override
        public String getPrincipal() {
            return name;
        }

        @Override
        public boolean isUserInRole(String s) {
            return true;
        }
    };

    public NotificationHook(String name) {
        this(name, null);
    }

    public NotificationHook(String name, Mediator mediator) {
        this(name, new ObjectMapper(), JsonNodeFactory.withExactBigDecimals(true), mediator);
    }

    public NotificationHook(String name, ObjectMapper objectMapper,
            JsonNodeFactory jsonNodeFactory, Mediator mediator) {
        this.name = name;
        this.jsonNodeFactory = jsonNodeFactory;
        this.objectMapper = objectMapper;
        this.mediator = mediator;
    }

    @Override
    public void setLightblueFactory(LightblueFactory lightblueFactory) {
        synchronized (this) {
            this.lightblueFactory = lightblueFactory;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void processHook(EntityMetadata entityMetadata,
                            HookConfiguration hookConfiguration,
                            List<HookDoc> hookDocs) {
        if (hookConfiguration == null) {
            LOGGER.warn("No notificationHook configuration provided, assuming you want to watch "
                    + "all fields and include only IDs.");
            hookConfiguration = NotificationHookConfiguration.watchingEverythingAndIncludingNothing();
        } else if (!(hookConfiguration instanceof NotificationHookConfiguration)) {
            throw new IllegalArgumentException("Expected instance of " +
                    "NotificationHookConfiguration but got: " + hookConfiguration);
        }

        NotificationHookConfiguration config = (NotificationHookConfiguration) hookConfiguration;
        Mediator mediator = tryGetMediator();

        Projection watchProjection=addArrayIdentities(config.watchProjection(),entityMetadata);
        Projector watchProjector = Projector.getInstance(watchProjection, entityMetadata);
        Projector includeProjector = Projector.getInstance(config.includeProjection(), entityMetadata);
        

        for (HookDoc hookDoc : hookDocs) {
            HookResult result = processSingleHookDoc(entityMetadata,
                                                     watchProjector,
                                                     includeProjector,
                                                     config.isArrayOrderingSignificant(),
                                                     hookDoc,
                                                     mediator);
            
            // TODO: batch
            if(result.hasException()) {
                throw new NotificationProcessingError(result.exception);
            } else if (result.hasErrorsOrDataErrors()) {                
                throw new NotificationInsertErrorsException(result.entity, result.errors,
                                                            result.dataErrors);
            }
        }
    }

    private boolean isProjected(Path field,Projection p) {
        switch(p.getFieldInclusion(field)) {
        case explicit_inclusion:
        case implicit_inclusion:return true;
        default: return false;
        }
    }

    private Projection addArrayIdentities(Projection p,EntityMetadata md) {
        // If an array is included in the projection, make sure its identity is also included
        Map<Path,List<Path>> arrayIdentities=md.getEntitySchema().getArrayIdentities();
        List<Projection> addFields=new ArrayList<>();
        for(Map.Entry<Path,List<Path>> entry:arrayIdentities.entrySet()) {
            Path array=entry.getKey();
            List<Path> identities=new ArrayList<>();
            for(Path x:entry.getValue())
                identities.add(new Path(array,new Path(Path.ANYPATH,x)));
            
            if(isProjected(array,p)) {
                for(Path id:identities) {
                    if(!isProjected(id,p)) {
                        addFields.add(new FieldProjection(id,true,true));
                    }
                }
            }
        }
        if(!addFields.isEmpty()) {
            LOGGER.debug("Excluded array identities are added to projection:{}",addFields);
            // Need to first add the original projection, then the included fields.
            // This is order sensitive
            return Projection.add(p,new ProjectionList(addFields));
        } else
            return p;
    }
    
    private HookResult processSingleHookDoc(EntityMetadata metadata,
                                            Projector watchProjector,
                                            Projector includeProjector,
                                            boolean arrayOrderingSignificant,
                                            HookDoc hookDoc,
                                            Mediator mediator) {
        LOGGER.debug("Processing doc starts");
        JsonDoc postDoc = hookDoc.getPostDoc();
        JsonDoc preDoc = hookDoc.getPreDoc();

        // TODO(ahenning): Support delete
        if (hookDoc.getCRUDOperation().equals(CRUDOperation.FIND) || postDoc == null) {
            return HookResult.aborted();
        }

        try {
            DocComparator.Difference<JsonNode> diff=compareDocs(metadata,preDoc,postDoc,watchProjector);
            if(!diff.same()) {
                if(diff.getNumChangedFields()>0 || arrayOrderingSignificant) {                
                    LOGGER.debug("Watched fields changed, creating notification");
                    NotificationEntity notification =
                        makeNotificationEntityWithIncludedFields(hookDoc, includeProjector, diff, arrayOrderingSignificant);
                    
                    EntityVersion notificationVersion = new EntityVersion(NotificationEntity.ENTITY_NAME,
                                                                          NotificationEntity.ENTITY_VERSION);
                    
                    InsertionRequest newNotification = new InsertionRequest();
                    newNotification.setClientId(notificationHookClientId);
                    newNotification.setEntityVersion(notificationVersion);
                    newNotification.setEntityData(objectMapper.valueToTree(notification));
                    
                    LOGGER.debug("Inserting notification");
                    Response response = mediator.insert(newNotification);
                    
                    return new HookResult(notification, response.getErrors(), response.getDataErrors());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error processing hook:"+e);
            return HookResult.exception(e);
        }
        return HookResult.aborted();
    }

    /**
     * Compares the pre- and post- documents after they are passed
     * through the watchProjector, and returns the delta
     */
    private DocComparator.Difference<JsonNode> compareDocs(EntityMetadata metadata,
                                                           JsonDoc preDoc,
                                                           JsonDoc postDoc,
                                                           Projector watchProjector)
        throws Exception {
        JsonDoc watchedPostDoc = watchProjector.project(postDoc, jsonNodeFactory);
        JsonDoc watchedPreDoc = preDoc == null
            ? new JsonDoc(jsonNodeFactory.objectNode())
            : watchProjector.project(preDoc, jsonNodeFactory);
        
        // Compute diff
        JsonCompare cmp=metadata.getDocComparator();
        LOGGER.debug("Array identities:{}",cmp.getArrayIdentities());
        LOGGER.debug("Pre:{}, Post:{}",watchedPreDoc.getRoot(),watchedPostDoc.getRoot());
        DocComparator.Difference<JsonNode> diff=cmp.
            compareNodes(watchedPreDoc.getRoot(),watchedPostDoc.getRoot());
        LOGGER.debug("Diff: {}",diff);
        return diff;
    }

    private NotificationEntity makeNotificationEntityWithIncludedFields(HookDoc hookDoc,
                                                                        Projector includeProjector,
                                                                        DocComparator.Difference<JsonNode> diff,
                                                                        boolean arrayOrderSignificant) {
        EntityMetadata metadata = hookDoc.getEntityMetadata();
        JsonDoc postDoc = hookDoc.getPostDoc();

        List<PathAndValue> entityData=new ArrayList<>();
        List<PathAndValue> removedEntityData = new ArrayList<>();
        List<String> updatedPaths = new ArrayList<>();
        List<String> removedPaths = new ArrayList<>();
        boolean isInsert = hookDoc.getPreDoc() == null;

        // Add entity identities to entity data
        for (Field identityField : metadata.getEntitySchema().getIdentityFields()) {
            Path identityPath = identityField.getFullPath();
            String pathString = identityPath.toString();
            String valueString = postDoc.get(identityPath).asText(null);
            entityData.add(new PathAndValue(pathString, valueString));
        }

        // Add flattened include doc to entity data
        JsonDoc includeDoc=includeProjector.project(postDoc,jsonNodeFactory);
        flatten("", includeDoc.getRoot(), entityData);

        // Add updates to entity data, removed entity data, updated paths, and removed paths
        for(DocComparator.Delta<JsonNode> delta : diff.getDelta()) {
            if (delta instanceof DocComparator.Move && arrayOrderSignificant) {
                JsonNode movedNode = ((DocComparator.Move<JsonNode>) delta).getMovedNode();
                String newPath = delta.getField2().toString();

                updatedPaths.add(newPath);
                flatten(newPath, movedNode, entityData);
            } else if (delta instanceof DocComparator.Removal) {
                JsonNode removedNode=((DocComparator.Removal<JsonNode>)delta).getRemovedNode();
                String removedPath = delta.getField().toString();

                if(removedNode.isContainerNode()) {
                    removedPaths.add(removedPath);
                    flatten(removedPath, removedNode, removedEntityData);
                } else {
                    removedEntityData.add(new PathAndValue(removedPath, removedNode.asText(null)));
                }
            } else if (delta instanceof DocComparator.Addition) {
                JsonNode addedNode = ((DocComparator.Addition<JsonNode>) delta).getAddedNode();
                String addedPath = delta.getField().toString();

                updatedPaths.add(addedPath);

                if (addedNode.isContainerNode()) {
                    flatten(addedPath, addedNode, entityData);
                } else {
                    entityData.add(new PathAndValue(addedPath, addedNode.asText(null)));
                }
            } else if (delta instanceof DocComparator.Modification) {
                DocComparator.Modification<JsonNode> modification =
                    (DocComparator.Modification<JsonNode>) delta;
                String modifiedPath = delta.getField2().toString();

                String modifiedValue = modification.getModifiedNode().asText(null);
                String unmodifiedValue = modification.getUnmodifiedNode().asText(null);

                updatedPaths.add(modifiedPath);
                entityData.add(new PathAndValue(modifiedPath, modifiedValue));
                removedEntityData.add(new PathAndValue(modifiedPath, unmodifiedValue));
            }
        }

        // Now we have the pieces, construct the notification to serialize.
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setUpdatedPaths(updatedPaths);
        notificationEntity.setRemovedEntityData(removedEntityData);
        notificationEntity.setRemovedPaths(removedPaths);
        notificationEntity.setEntityData(entityData);

        // TODO(ahenning): Support delete
        NotificationEntity.Operation operation = isInsert
            ? NotificationEntity.Operation.insert
            : NotificationEntity.Operation.update;

        notificationEntity.setEntityName(metadata.getName());
        notificationEntity.setEntityVersion(metadata.getVersion().getValue());
        notificationEntity.setOperation(operation);
        notificationEntity.setClientRequestPrincipal(hookDoc.getWho());
        notificationEntity.setClientRequestDate(hookDoc.getWhen());
        notificationEntity.setStatus(NotificationEntity.Status.unprocessed);
        
        return notificationEntity;
    }

    private void flatten(String prefix, JsonNode node, List<PathAndValue> entityData) {
        if (node.size() == 0) {
            return;
        }

        JsonNodeCursor cursor = new JsonNodeCursor(Path.EMPTY, node);

        while(cursor.next()) {
            String p=cursor.getCurrentPath().toString();
            JsonNode value=cursor.getCurrentNode();

            if(value.isValueNode()) {
                String path = prefix.isEmpty() ? p : (prefix + "." + p);
                PathAndValue data = new PathAndValue(path, value.asText(null));

                // TODO(ahenning): Consider using Set instead of List for entityData
                if (!entityData.contains(data)) {
                    entityData.add(data);
                }
            }
        }
    }

    // TODO(ahenning): This messiness can be removed if we can inject the lightblue factory in
    // the parser instead of the hook. Then hook can accept mediator in constructor and we only
    // validate it is non null and that's it.
    // See: https://github.com/lightblue-platform/lightblue-core/pull/587
    protected Mediator tryGetMediator() {
        if (mediator == null) {
            synchronized (this) {
                if (mediator == null) {
                    if (lightblueFactory == null) {
                        throw new IllegalStateException("No Mediator or LightblueFactory provided!");
                    }

                    try {
                        mediator = lightblueFactory.getMediator();
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to get Mediator from LightblueFactory!", e);
                    }
                }
            }
        }
        return mediator;
    }

    static class HookResult {
        final NotificationEntity entity;
        final List<Error> errors;
        final List<DataError> dataErrors;
        final Exception exception;

        static HookResult aborted() {
            return new HookResult(null, Collections.<Error>emptyList(),
                                  Collections.<DataError>emptyList(),null);
        }

        static HookResult exception(Exception x) {
            return new HookResult(null, Collections.<Error>emptyList(),
                                  Collections.<DataError>emptyList(),x);
        }

        HookResult(NotificationEntity entity, List<Error> errors, List<DataError> dataErrors, Exception exception) {
            this.entity = entity;
            this.errors = errors;
            this.dataErrors = dataErrors;
            this.exception = exception;
        }

        HookResult(NotificationEntity entity, List<Error> errors, List<DataError> dataErrors) {
            this(entity,errors,dataErrors,null);
        }

        boolean hasErrorsOrDataErrors() {
            return !errors.isEmpty() || !dataErrors.isEmpty();
        }

        boolean hasException() {
            return exception!=null;
        }
    }
}
