package org.esbtools.lightbluenotificationhook;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class EntityNotificationHookConfigurationReader {
    /**
     * Reads entity notification hook configuration from a given class via reflectively examining
     * its static fields.
     *
     * <p>This method should be used as the source of truth for how this configuration is attached
     * to a given class.
     *
     * @see GeneratesNotificationHookConfiguration
     */
    public EntityNotificationHookConfiguration readConfiguration(
            Class<?> notification) throws IllegalAccessException {
        for (Field field : notification.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) &&
                    field.isAnnotationPresent(GeneratesNotificationHookConfiguration.class)) {
                field.setAccessible(true);

                if (!EntityNotificationHookConfiguration.class.isAssignableFrom(field.getType())) {
                    throw new IllegalArgumentException("Static field annotated with " +
                            GeneratesNotificationHookConfiguration.class + " does not implement " +
                            EntityNotificationHookConfiguration.class);
                }

                EntityNotificationHookConfiguration config =
                        (EntityNotificationHookConfiguration) field.get(null);

                if (config == null) {
                    throw new NullPointerException("Found static " +
                            EntityNotificationHookConfiguration.class + " field annotated with " +
                            GeneratesNotificationHookConfiguration.class + " but it was null.");
                }

                return config;
            }
        }

        throw new IllegalArgumentException("No static " +
                EntityNotificationHookConfiguration.class + " field found annotated with " +
                GeneratesNotificationHookConfiguration.class + " on class " + notification + " " +
                "when trying to generate notification hook configuration.");
    }
}
