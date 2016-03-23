package org.esbtools.lightbluenotificationhook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.jimfs.Jimfs;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

@RunWith(JUnit4.class)
public class MainTest {
    FileSystem fileSystem = Jimfs.newFileSystem();
    Main main = Main.usingLightblueDefaults();
    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void shouldWriteHookConfiguration() throws IllegalAccessException, IOException,
            ClassNotFoundException {
        Files.createDirectory(fileSystem.getPath("test/"));

        main.writeHookConfigurationForClass(
                ExampleNotification.class.getName(),
                MainTest.class.getClassLoader(),
                fileSystem.getPath("test/"));

        Path expectedResult = fileSystem.getPath("test/exampleNotificationHookConfiguration.json");
        JsonNode jsonConfig = mapper.readTree(Files.newBufferedReader(expectedResult));

        assertEquals("watchedField", jsonConfig.get("watchProjection").get("field").textValue());
        assertEquals("includedField", jsonConfig.get("includeProjection").get("field").textValue());
        assertTrue(jsonConfig.get("arrayOrderingSignificant").booleanValue());
    }
}
