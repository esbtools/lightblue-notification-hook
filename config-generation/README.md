# Lightblue notification hook configuration generation

Writing logic for notifications of a certain entity is necessarily coupled to 
notification hook configuration for that entity. However, the configuration 
lives in an instance of lightblue. One would like to keep a single source of 
truth for this configuration close to the logic of the notification itself, 
which is primarily what decides what needs to go in the configurations' watch
and include projections, rather than maintaining that far from the code, in 
which case keeping those in sync would be error prone at least.

Using config generation, we can write this configuration as a static field on
the notification implementation itself (such as one written using the 
[lightblue event handler](https://github.com/esbtools/event-handler#lightblue))
and use this to generate the json to put inside the entity's metadata's hook 
configuration.

## How it works

Configuration is read via reflection on a given class. If a static field is
found that implements `EntityNotificationHookConfiguration`, this will be 
used to generate the JSON. You can create one of these instances using a 
builder on the same interface. It uses the [lightblue 
client](https://github.com/lightblue-platform/lightblue-client) `Projection`
types in order to conveniently define arbitrarily complicated projections.

You can use the library directly or as a CLI Java application. Typically you
will want to use this as a CLI application that runs at build time, outputting
JSON you can use to update lightblue entity metadata as a part of deploying 
your application. This CLI application expects to run with any classes being
introspected (as well as their dependencies) already on the classpath.
Fortunately, both of these goals are easy to achieve via the 
[exec-maven-plugin](http://www.mojohaus.org/exec-maven-plugin/).

## Example

See [the tests][1] for an example configured notification.

Here is an example pom build segment:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>${version.exec-maven-plugin}</version>
            <executions>
                <execution>
                    <phase>process-classes</phase>
                    <goals>
                        <goal>java</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <includeProjectDependencies>true</includeProjectDependencies>
                <includePluginDependencies>true</includePluginDependencies>
                <executableDependency>
                    <groupId>org.esbtools.lightblue-notification-hook</groupId>
                    <artifactId>lightblue-notification-hook-config-generation-bin</artifactId>
                </executableDependency>
                <mainClass>org.esbtools.lightbluenotificationhook.Main</mainClass>
                <arguments>
                    <argument>${project.build.outputDirectory}</argument>
                    <!-- Add notification classes to scan here, like so: -->
                    <argument>my.cool.EntityNotification</argument>
                    <argument>my.cool.OtherNotification</argument>
                </arguments>
            </configuration>
            <dependencies>
                <dependency>
                    <groupId>org.esbtools.lightblue-notification-hook</groupId>
                    <artifactId>lightblue-notification-hook-config-generation-bin</artifactId>
                    <version>${version.lightblue-notification-hook-config-generation-bin}</version>
                    <type>jar</type>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
```

[1]: https://github.com/esbtools/lightblue-notification-hook/tree/master/config-generation/bin/src/test/java/org/esbtools/lightbluenotificationhook



