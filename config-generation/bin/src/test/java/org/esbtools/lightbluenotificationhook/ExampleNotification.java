package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.client.Projection;

public class ExampleNotification /* implements LightblueNotification */ {
    private static final EntityNotificationHookConfiguration hookConfiguration =
            EntityNotificationHookConfiguration.forEntity("example")
                    .watching(Projection.includeField("watchedField"))
                    .including(Projection.includeField("includedField"))
                    .arrayOrderingSignificant()
                    .build();
}
