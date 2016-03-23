package org.esbtools.lightbluenotificationhook;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static field instance of {@link EntityNotificationHookConfiguration} to be used for
 * notification hook configuration generation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface GeneratedNotificationHookConfiguration {
}
