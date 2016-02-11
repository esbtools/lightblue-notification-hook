package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.Response;
import com.redhat.lightblue.crud.CRUDOperation;
import com.redhat.lightblue.crud.InsertionRequest;
import com.redhat.lightblue.hooks.HookDoc;
import com.redhat.lightblue.mediator.Mediator;
import com.redhat.lightblue.metadata.DataStore;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.metadata.PredefinedFields;
import com.redhat.lightblue.metadata.TypeResolver;
import com.redhat.lightblue.metadata.parser.DataStoreParser;
import com.redhat.lightblue.metadata.parser.Extensions;
import com.redhat.lightblue.metadata.parser.JSONMetadataParser;
import com.redhat.lightblue.metadata.parser.MetadataParser;
import com.redhat.lightblue.metadata.types.DefaultTypes;
import com.redhat.lightblue.query.Projection;
import com.redhat.lightblue.util.JsonDoc;
import com.redhat.lightblue.util.JsonUtils;
import com.redhat.lightblue.util.Path;
import com.redhat.lightblue.util.test.AbstractJsonSchemaTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class NotificationHookTest extends AbstractJsonSchemaTest {
    public static class InsertCapturingMediator extends Mediator {
        public InsertionRequest capturedInsert;
        public Response response = new Response();
        
        public InsertCapturingMediator() {
            super(null,null);
        }

        public Response insert(InsertionRequest req) {
            this.capturedInsert = req;
            return response;
        }
    }

    private InsertCapturingMediator insertCapturingMediator = new InsertCapturingMediator();
    private NotificationHook hook = new NotificationHook("testHook", insertCapturingMediator);

    @Test
    public void shouldCreateNotificationWithIdsForEntityInsertsWhichContainWatchedFieldsWhenNoIncludeProjectionConfigured() throws Exception {
        EntityMetadata md = getMd("usermd.json");
        JsonNode data = loadJsonNode("userdata.json");

        HookConfiguration cfg = new NotificationHookConfiguration(
                // Watch anything under "personalInfo"
                projection("{'field':'personalInfo','recursive':1}"),
                // Only store id in notification
                null,
                // Ignore array order
                false);

        // Insertion
        List<HookDoc> docs = new ArrayList<>();
        HookDoc doc = new HookDoc(md,null,new JsonDoc(data),CRUDOperation.INSERT,"me");
        docs.add(doc);

        hook.processHook(md, cfg, docs);

        Assert.assertNotNull("Hook did not insert any notification", insertCapturingMediator.capturedInsert);
        JsonNode notification = insertCapturingMediator.capturedInsert.getEntityData();
        Assert.assertNotNull("Hook tried to insert a null notification", notification);
        System.out.println(notification);
        Assert.assertEquals("user", notification.get("entityName").asText());
        Assert.assertEquals("5.0.0", notification.get("entityVersion").asText());
        Assert.assertEquals("insert", notification.get("operation").asText());
        ArrayNode entityData = (ArrayNode) notification.get("entityData");
        Assert.assertEquals("Captured more or less entity data than expected", 2, entityData.size());
        assertEntityDataValueEquals(entityData,"_id","123");
        assertEntityDataValueEquals(entityData,"iduid","345");
    }

    
    @Test
    public void shouldCreateNotificationWithIdsForEntityUpdatesWhenWatchedFieldChangesAndNoIncludeProjectionProvided() throws Exception {
        EntityMetadata md=getMd("usermd.json");
        JsonNode pre=loadJsonNode("userdata.json");
        // Watch anything under "personalInfo"
        // Only store id in notification
        HookConfiguration cfg=new NotificationHookConfiguration(
                projection("{'field':'personalInfo','recursive':1}"),
                null,
                false);

        JsonNode post=loadJsonNode("userdata.json");
        List<HookDoc> docs = new ArrayList<>();
        JsonDoc.modify(post,new Path("personalInfo.company"),JsonNodeFactory.instance.textNode("blah"),true);
        HookDoc doc=new HookDoc(md,new JsonDoc(pre),new JsonDoc(post),CRUDOperation.UPDATE,"me");
        docs.add(doc);

        hook.processHook(md,cfg,docs);

        Assert.assertNotNull("Hook did not insert any notification", insertCapturingMediator.capturedInsert);
        JsonNode notification= insertCapturingMediator.capturedInsert.getEntityData();
        Assert.assertNotNull("Hook tried to insert a null notification", notification);
        System.out.println(notification);
        Assert.assertEquals("user",notification.get("entityName").asText());
        Assert.assertEquals("5.0.0",notification.get("entityVersion").asText());
        Assert.assertEquals("update", notification.get("operation").asText());
        // The id fields must be there, and nothing else
        ArrayNode ed=(ArrayNode)notification.get("entityData");
        Assert.assertEquals(2,ed.size());
        assertEntityDataValueEquals(ed,"_id","123");
        assertEntityDataValueEquals(ed,"iduid","345");
    }


    @Test
    public void shouldCaptureIncludeProjectionInEntityDataInAdditionToIds() throws Exception {
        EntityMetadata md=getMd("usermd.json");
        JsonNode pre=loadJsonNode("userdata.json");

        HookConfiguration cfg=new NotificationHookConfiguration(
                projection("{'field':'personalInfo','recursive':1}"),
                projection("[{'field':'login'},{'field':'sites.*.siteType'}]"),
                false);
        
        JsonNode post=loadJsonNode("userdata.json");
        JsonDoc.modify(post,new Path("personalInfo.company"),JsonNodeFactory.instance.textNode("blah"),true);
        
        List<HookDoc> docs=new ArrayList<>();
        HookDoc doc=new HookDoc(md,new JsonDoc(pre),new JsonDoc(post),CRUDOperation.UPDATE,"me");
        docs.add(doc);

        hook.processHook(md,cfg,docs);

        Assert.assertNotNull(insertCapturingMediator.capturedInsert);
        // Doc is in the insertion req
        JsonNode notification= insertCapturingMediator.capturedInsert.getEntityData();
        Assert.assertNotNull(notification);
        System.out.println(notification);
        ArrayNode ed=(ArrayNode)notification.get("entityData");
        Assert.assertEquals(5,ed.size());
        assertEntityDataValueEquals(ed,"_id","123");
        assertEntityDataValueEquals(ed,"iduid","345");
        assertEntityDataValueEquals(ed,"login","bserdar");
        assertEntityDataValueEquals(ed,"sites.0.siteType","shipping");
        assertEntityDataValueEquals(ed,"sites.1.siteType","billing");
    }

    @Test
    public void shouldNotCreateNotificationForFindOperations() throws Exception {
        EntityMetadata md = getMd("usermd.json");
        JsonNode post = loadJsonNode("userdata.json");

        HookConfiguration cfg = NotificationHookConfiguration.watchingEverythingAndIncludingNothing();

        List<HookDoc> docs = new ArrayList<>();
        // Pre doc is null in find ops: see com.redhat.lightblue.hooks.HookManager:73
        HookDoc doc = new HookDoc(md,null,new JsonDoc(post),CRUDOperation.FIND,"me");
        docs.add(doc);

        hook.processHook(md, cfg, docs);

        Assert.assertNull(insertCapturingMediator.capturedInsert);
    }

    @Test
    public void shouldNotCreateNotificationIfNothingChangesInWatchedFields() throws Exception {
        EntityMetadata md = getMd("usermd.json");
        JsonNode pre = loadJsonNode("userdata.json");
        JsonNode post = loadJsonNode("userdata.json");

        HookConfiguration cfg = NotificationHookConfiguration.watchingEverythingAndIncludingNothing();

        List<HookDoc> docs = new ArrayList<>();
        HookDoc doc = new HookDoc(md,new JsonDoc(pre),new JsonDoc(post),CRUDOperation.UPDATE,"me");
        docs.add(doc);

        hook.processHook(md, cfg, docs);

        Assert.assertNull(insertCapturingMediator.capturedInsert);
    }

    @Test
    public void shouldNotCreateNotificationsIfSomethingNotWatchedChanged() throws Exception {
        EntityMetadata md = getMd("usermd.json");
        JsonNode pre = loadJsonNode("userdata.json");

        HookConfiguration cfg = new NotificationHookConfiguration(
                projection("{'field':'personalInfo','recursive':1}"),
                null,
                false);

        JsonNode post = loadJsonNode("userdata.json");
        JsonDoc.modify(post, new Path("login"), JsonNodeFactory.instance.textNode("blah"), true);

        List<HookDoc> docs= new ArrayList<>();
        HookDoc doc = new HookDoc(md, new JsonDoc(pre), new JsonDoc(post), CRUDOperation.UPDATE, "me");
        docs.add(doc);

        hook.processHook(md,cfg,docs);

        Assert.assertNull(insertCapturingMediator.capturedInsert);
    }

    @Test
    public void shouldNotCreateNotificationForInsertWhichDoesNotIncludeAnyWatchedFields() throws Exception {
        EntityMetadata md = getMd("usermd.json");

        HookConfiguration cfg = new NotificationHookConfiguration(
                projection("{'field':'contactPermissions','recursive':1}"),
                null,
                false);

        JsonNode post = loadJsonNode("userdataWithoutContactPermissions.json");

        List<HookDoc> docs= new ArrayList<>();
        HookDoc doc = new HookDoc(md, null, new JsonDoc(post), CRUDOperation.INSERT, "me");
        docs.add(doc);

        hook.processHook(md,cfg,docs);

        Assert.assertNull(insertCapturingMediator.capturedInsert);
    }

    private void assertEntityDataValueEquals(ArrayNode ed, String path, String value) {
        int n=ed.size();
        for(int i=0;i<n;i++) {
            ObjectNode node=(ObjectNode)ed.get(i);
            JsonNode p=node.get("path");
            JsonNode v=node.get("value");
            if(p!=null&&v!=null) {
                if(path.equals(p.asText())&&value.equals(v.asText()))
                    return;
            }
        }
        Assert.fail("Expected to find "+path+":"+value);
    }

    private Projection projection(String s) throws Exception {
        return Projection.fromJson(JsonUtils.json(s.replaceAll("\'","\"")));
    }

    public EntityMetadata getMd(String fname) throws Exception {
        JsonNode node = loadJsonNode(fname);
        Extensions<JsonNode> extensions = new Extensions<>();
        extensions.addDefaultExtensions();
        extensions.registerDataStoreParser("mongo", new FakeMongoDataStoreParser<JsonNode>());
        TypeResolver resolver = new DefaultTypes();
        JSONMetadataParser parser = new JSONMetadataParser(extensions, resolver, JsonNodeFactory.instance);
        EntityMetadata md = parser.parseEntityMetadata(node);
        PredefinedFields.ensurePredefinedFields(md);
        return md;
    }

    public class FakeMongoDataStoreParser<T> implements DataStoreParser<T> {

        @Override
        public DataStore parse(String name, MetadataParser<T> p, T node) {
            return new DataStore() {
                public String getBackend() {
                    return "mongo";
                }
            };
        }

        @Override
        public void convert(MetadataParser<T> p, T emptyNode, DataStore object) {
        }

        @Override
        public String getDefaultName() {
            return "mongo";
        }
    }
}
