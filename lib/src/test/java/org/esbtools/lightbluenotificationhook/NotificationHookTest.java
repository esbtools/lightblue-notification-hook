package org.esbtools.lightbluenotificationhook;

import java.util.List;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import com.redhat.lightblue.*;
import com.redhat.lightblue.crud.*;
import com.redhat.lightblue.mediator.*;
import com.redhat.lightblue.query.*;
import com.redhat.lightblue.util.*;
import com.redhat.lightblue.metadata.*;
import com.redhat.lightblue.hooks.HookDoc;
import com.redhat.lightblue.metadata.parser.Extensions;
import com.redhat.lightblue.metadata.parser.JSONMetadataParser;
import com.redhat.lightblue.metadata.parser.DataStoreParser;
import com.redhat.lightblue.metadata.parser.MetadataParser;
import com.redhat.lightblue.metadata.types.DefaultTypes;

import com.redhat.lightblue.util.test.AbstractJsonSchemaTest;

public class NotificationHookTest extends AbstractJsonSchemaTest {

    public class TestNotificationHook extends NotificationHook {
        public TestNotificationHook(String name) {
            super(name);
        }

        protected Mediator tryGetMediator() {
            return mediator;
        }
    }

    public class TestMediator extends Mediator {

        public InsertionRequest req;
        public Response response=new Response();
        
        public TestMediator() {
            super(null,null);
        }

        public Response insert(InsertionRequest req) {
            this.req=req;
            return response;
        }
    }

    
    public class TestDataStoreParser<T> implements DataStoreParser<T> {
        
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
    
    private TestNotificationHook hook=new TestNotificationHook("testHook");
    private TestMediator mediator=new TestMediator();

    private Projection projection(String s) throws Exception {
        return Projection.fromJson(JsonUtils.json(s.replaceAll("\'","\"")));
    }

    public EntityMetadata getMd(String fname) throws Exception {
        JsonNode node = loadJsonNode(fname);
        Extensions<JsonNode> extensions = new Extensions<>();
        extensions.addDefaultExtensions();
        extensions.registerDataStoreParser("mongo", new TestDataStoreParser<JsonNode>());
        TypeResolver resolver = new DefaultTypes();
        JSONMetadataParser parser = new JSONMetadataParser(extensions, resolver, JsonNodeFactory.instance);
        EntityMetadata md = parser.parseEntityMetadata(node);
        PredefinedFields.ensurePredefinedFields(md);
        return md;
    }

    @Test
    public void testIns() throws Exception {
        EntityMetadata md=getMd("usermd.json");
        JsonNode data=loadJsonNode("userdata.json");
        // Watch anything under "personalInfo"
        // Only store id in notification
        HookConfiguration cfg=new NotificationHookConfiguration(projection("{'field':'personalInfo','recursive':1}"),
                                                                null,
                                                                false);

        // Insertion
        List<HookDoc> docs=new ArrayList<>();
        HookDoc doc=new HookDoc(md,null,new JsonDoc(data),CRUDOperation.INSERT,"me");
        docs.add(doc);
        mediator.req=null;
        hook.processHook(md,cfg,docs);
        // There must be an insertion request
        Assert.assertNotNull(mediator.req);
        // Doc is in the insertion req
        JsonNode notification=mediator.req.getEntityData();
        Assert.assertNotNull(notification);
        System.out.println(notification);
        Assert.assertEquals("user",notification.get("entityName").asText());
        Assert.assertEquals("5.0.0",notification.get("entityVersion").asText());
        // The id fields must be there, and nothing else
        ArrayNode ed=(ArrayNode)notification.get("entityData");
        Assert.assertEquals(2,ed.size());
        checkEntityData(ed,"_id","123");
        checkEntityData(ed,"iduid","345");
    }

    
    @Test
    public void testUpd() throws Exception {
        EntityMetadata md=getMd("usermd.json");
        JsonNode pre=loadJsonNode("userdata.json");
        // Watch anything under "personalInfo"
        // Only store id in notification
        HookConfiguration cfg=new NotificationHookConfiguration(projection("{'field':'personalInfo','recursive':1}"),
                                                                null,
                                                                false);

        // Update something other than personalinfo
        JsonNode post=loadJsonNode("userdata.json");
        JsonDoc.modify(post,new Path("login"),JsonNodeFactory.instance.textNode("blah"),true);
        
        List<HookDoc> docs=new ArrayList<>();
        HookDoc doc=new HookDoc(md,new JsonDoc(pre),new JsonDoc(post),CRUDOperation.UPDATE,"me");
        docs.add(doc);
        mediator.req=null;
        hook.processHook(md,cfg,docs);
        // No request
        Assert.assertNull(mediator.req);

        // Update something under personalInfo
        JsonDoc.modify(post,new Path("personalInfo.company"),JsonNodeFactory.instance.textNode("blah"),true);
        docs=new ArrayList<>();
        doc=new HookDoc(md,new JsonDoc(pre),new JsonDoc(post),CRUDOperation.UPDATE,"me");
        docs.add(doc);
        mediator.req=null;
        hook.processHook(md,cfg,docs);
        // There must be an insertion request
        Assert.assertNotNull(mediator.req);
        // Doc is in the insertion req
        JsonNode notification=mediator.req.getEntityData();
        Assert.assertNotNull(notification);
        System.out.println(notification);
        Assert.assertEquals("user",notification.get("entityName").asText());
        Assert.assertEquals("5.0.0",notification.get("entityVersion").asText());
        // The id fields must be there, and nothing else
        ArrayNode ed=(ArrayNode)notification.get("entityData");
        Assert.assertEquals(2,ed.size());
        checkEntityData(ed,"_id","123");
        checkEntityData(ed,"iduid","345");
    }


    @Test
    public void testMoreFields() throws Exception {
        EntityMetadata md=getMd("usermd.json");
        JsonNode pre=loadJsonNode("userdata.json");
        // Watch anything under "personalInfo"
        // Only store id in notification
        HookConfiguration cfg=new NotificationHookConfiguration(projection("{'field':'personalInfo','recursive':1}"),
                                                                projection("[{'field':'login'},{'field':'sites.*.siteType'}]"),
                                                                false);
        
        JsonNode post=loadJsonNode("userdata.json");
        JsonDoc.modify(post,new Path("personalInfo.company"),JsonNodeFactory.instance.textNode("blah"),true);
        
        List<HookDoc> docs=new ArrayList<>();
        HookDoc doc=new HookDoc(md,new JsonDoc(pre),new JsonDoc(post),CRUDOperation.UPDATE,"me");
        docs.add(doc);
        mediator.req=null;
        hook.processHook(md,cfg,docs);

        // There must be an insertion request
        Assert.assertNotNull(mediator.req);
        // Doc is in the insertion req
        JsonNode notification=mediator.req.getEntityData();
        Assert.assertNotNull(notification);
        System.out.println(notification);
        ArrayNode ed=(ArrayNode)notification.get("entityData");
        Assert.assertEquals(5,ed.size());
        checkEntityData(ed,"_id","123");
        checkEntityData(ed,"iduid","345");
        checkEntityData(ed,"login","bserdar");
        checkEntityData(ed,"sites.0.siteType","shipping");
        checkEntityData(ed,"sites.1.siteType","billing");
    }

    private void checkEntityData(ArrayNode ed,String path,String value) {
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
}
