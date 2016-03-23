package org.esbtools.lightbluenotificationhook;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class EntityNotificationHookConfigurationReader {
    /**
     * Reads entity notification hook configuration from a given class via reflectively examining
     * its static fields. The first static field which is non-null, implements
     * {@link EntityNotificationHookConfiguration}, and is annotated with
     * {@link GeneratedNotificationHookConfiguration} is returned.
     *
     * <p>This method should be used as the source of truth for how this configuration is attached
     * to a given class.
     */
    public EntityNotificationHookConfiguration readConfiguration(
            Class<?> notification) throws IllegalAccessException {
        for (Field field : notification.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) &&
                    field.isAnnotationPresent(GeneratedNotificationHookConfiguration.class)) {
                field.setAccessible(true);

                if (!EntityNotificationHookConfiguration.class.isAssignableFrom(field.getType())) {
                    throw new IllegalArgumentException("Static field annotated with " +
                            GeneratedNotificationHookConfiguration.class + " does not implement " +
                            EntityNotificationHookConfiguration.class);
                }

                EntityNotificationHookConfiguration config =
                        (EntityNotificationHookConfiguration) field.get(null);

                if (config == null) {
                    continue;
                }

                return config;
            }
        }

        throw new IllegalArgumentException("No static field found with non-null instance of " +
                EntityNotificationHookConfiguration.class + " on class " + notification + " when " +
                "trying to generate notification hook configuration.");
    }
}
