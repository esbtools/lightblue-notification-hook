package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.client.Projection;

class DefaultEntityNotificationHookConfiguration implements EntityNotificationHookConfiguration {
    private final String entityName;
    private final Projection watchProjection;
    private final Projection includeProjection;
    private final boolean arrayOrderingSignificant;

    DefaultEntityNotificationHookConfiguration(String entityName, Projection watchProjection,
            Projection includeProjection, boolean arrayOrderingSignificant) {
        this.entityName = entityName;
        this.watchProjection = watchProjection;
        this.includeProjection = includeProjection;
        this.arrayOrderingSignificant = arrayOrderingSignificant;
    }

    @Override
    public String entityName() {
        return entityName;
    }

    @Override
    public Projection watchProjection() {
        return watchProjection;
    }

    @Override
    public Projection includeProjection() {
        return includeProjection;
    }

    @Override
    public boolean arrayOrderingSignificant() {
        return arrayOrderingSignificant;
    }
}
