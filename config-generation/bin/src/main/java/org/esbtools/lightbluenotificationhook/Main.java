package org.esbtools.lightbluenotificationhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.metadata.parser.Extensions;
import com.redhat.lightblue.metadata.parser.JSONMetadataParser;
import com.redhat.lightblue.metadata.types.DefaultTypes;
import com.redhat.lightblue.query.Projection;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class Main {
    private final JSONMetadataParser parser;
    private final ObjectWriter jsonWriter;
    private final JsonNodeFactory jsonNodeFactory;
    private final EntityNotificationHookConfigurationReader generator;

    private static final String JSON_CONFIG_FILE_SUFFIX = "NotificationHookConfiguration.json";
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final ObjectWriter prettyWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

    public Main(JSONMetadataParser parser, ObjectWriter jsonWriter, JsonNodeFactory jsonNodeFactory,
            EntityNotificationHookConfigurationReader generator) {
        this.parser = parser;
        this.jsonWriter = jsonWriter;
        this.jsonNodeFactory = jsonNodeFactory;
        this.generator = generator;
    }

    public static Main usingLightblueDefaults() {
        EntityNotificationHookConfigurationReader generator = new EntityNotificationHookConfigurationReader();
        JsonNodeFactory factory = JsonNodeFactory.withExactBigDecimals(true);
        Extensions<JsonNode> extensions = new Extensions<>();
        extensions.addDefaultExtensions();
        JSONMetadataParser parser = new JSONMetadataParser(extensions, new DefaultTypes(), factory);

        return new Main(parser, prettyWriter, factory, generator);
    }

    public void writeHookConfigurationForClass(String classFqn, ClassLoader classLoader,
            Path outputPath) throws ClassNotFoundException, IllegalAccessException,
            IOException {
        Class notificationClass = Class.forName(classFqn, true, classLoader);

        EntityNotificationHookConfiguration entityConfig =
                generator.readConfiguration(notificationClass);

        Projection watch = Projection.fromJson(entityConfig.watchProjection().toJson());
        Projection include = Projection.fromJson(entityConfig.includeProjection().toJson());
        boolean arrayOrderingSignificant = entityConfig.arrayOrderingSignificant();

        NotificationHookConfiguration config =
                new NotificationHookConfiguration(watch, include, arrayOrderingSignificant);

        ObjectNode configAsJson = jsonNodeFactory.objectNode();
        config.toMetadata(parser, configAsJson);

        Path jsonResultPath = Files.isDirectory(outputPath)
                ? outputPath.resolve(entityConfig.entityName() + JSON_CONFIG_FILE_SUFFIX)
                : outputPath;

        jsonWriter.writeValue(Files.newBufferedWriter(jsonResultPath, UTF_8), configAsJson);
    }

    public static void main(String[] args) {
        List<String> helpArgs = Arrays.asList("-h", "h", "?", "-?", "help", "-help", "--help");

        if (args.length < 2 || helpArgs.contains(args[0].toLowerCase())) {
            printUsageAndExit();
        }

        Main main = Main.usingLightblueDefaults();
        ClassLoader mainClassLoader = Main.class.getClassLoader();

        try {
            for (int i = 1; i < args.length; i++) {
                String className = args[i];

                // TODO(ahenning): Make output path configurable
                main.writeHookConfigurationForClass(
                        className, mainClassLoader, Paths.get("target/generated-sources/"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            printUsageAndExit();
        }
    }

    private static void printUsageAndExit() {
        print("Usage: generator com.mycompany.Notification1 com.mycompany.Notification2 ...");
        print("Expects notifications to have a static field which is an instance of ");
        print(EntityNotificationHookConfiguration.class + ". This will be output as config.");
        print("");
        print("Also expects classpath to already contain these classes and these dependencies.");
        print("This is most natural to do by running from the maven project using the exec maven");
        print("plugin: http://www.mojohaus.org/exec-maven-plugin/examples/example-exec-using-plugin-dependencies.html");
        System.exit(1);
    }

    static void print(Object line) {
        System.out.println(line);
    }
}
