package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.client.Projection;

/**
 *
 */
public interface EntityNotificationHookConfiguration {
    String entityName();
    Projection watchProjection();
    Projection includeProjection();
    boolean arrayOrderingSignificant();

    static Builder forEntity(String entityName) {
        return new Builder(entityName);
    }

    final class Builder {
        private final String entityName;

        private Projection watchProjection = null;
        private Projection includeProjection = null;
        private boolean arrayOrderingSignificant = false;

        private Builder(String entityName) {
            this.entityName = entityName;
        }

        public Builder watching(Projection watchProjection) {
            this.watchProjection = watchProjection;
            return this;
        }

        public Builder including(Projection includeProjection) {
            this.includeProjection = includeProjection;
            return this;
        }

        public Builder arrayOrderingSignificant() {
            this.arrayOrderingSignificant = true;
            return this;
        }

        public EntityNotificationHookConfiguration build() {
            return new DefaultEntityNotificationHookConfiguration(entityName, watchProjection,
                    includeProjection, arrayOrderingSignificant);
        }
    }

}
