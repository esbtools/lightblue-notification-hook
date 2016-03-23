package org.esbtools.lightbluenotificationhook;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.lightblue.metadata.parser.Extensions;
import com.redhat.lightblue.metadata.parser.JSONMetadataParser;
import com.redhat.lightblue.metadata.types.DefaultTypes;
import com.redhat.lightblue.query.Projection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
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
    private final EntityNotificationHookConfigurationReader hookConfigReader;

    private static final String JSON_CONFIG_FILE_SUFFIX = "NotificationHookConfiguration.json";
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final ObjectWriter prettyWriter = new ObjectMapper()
            .writer(new DefaultPrettyPrinter()
                    .withArrayIndenter(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)
                    .withoutSpacesInObjectEntries());

    public Main(JSONMetadataParser parser, ObjectWriter jsonWriter, JsonNodeFactory jsonNodeFactory,
            EntityNotificationHookConfigurationReader hookConfigReader) {
        this.parser = parser;
        this.jsonWriter = jsonWriter;
        this.jsonNodeFactory = jsonNodeFactory;
        this.hookConfigReader = hookConfigReader;
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
        log.info("Looking up hook configuration for class: {}", classFqn);

        Class notificationClass = Class.forName(classFqn, true, classLoader);

        log.info("Found class: {}", classFqn);

        EntityNotificationHookConfiguration entityConfig =
                hookConfigReader.readConfiguration(notificationClass);

        log.info("Got configuration for entity: {}", entityConfig.entityName());

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

        log.info("Writing json configuration to: {}", jsonResultPath.toAbsolutePath());

        jsonWriter.writeValue(Files.newBufferedWriter(jsonResultPath, UTF_8), configAsJson);

        log.info("Wrote json configuration: {}", jsonResultPath);
    }

    public static void main(String[] args) throws FileNotFoundException {
        List<String> helpArgs = Arrays.asList("-h", "h", "?", "-?", "help", "-help", "--help");

        if (args.length < 2) {
            print("Not enough arguments provided, see usage:");
            printUsageAndExit();
        } else if (helpArgs.contains(args[0].toLowerCase())) {
            printUsageAndExit();
        }

        Main main = Main.usingLightblueDefaults();
        ClassLoader mainClassLoader = Main.class.getClassLoader();
        Path resultDir = Paths.get(args[0]);

        if (!Files.isDirectory(resultDir)) {
            throw new FileNotFoundException("Provided result directory does not exist.");
        }

        try {

            for (int i = 1; i < args.length; i++) {
                String className = args[i];

                main.writeHookConfigurationForClass(className, mainClassLoader, resultDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
            printUsageAndExit();
        }
    }

    private static void printUsageAndExit() {
        print("Usage: generator path/to/result/dir/ com.mycompany.Notification1 com.mycompany.Notification2 ...");
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
