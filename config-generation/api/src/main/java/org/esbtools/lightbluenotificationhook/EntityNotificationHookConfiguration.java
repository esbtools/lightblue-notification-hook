package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.client.Projection;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Defines notification hook configuration for a given entity using client-side constructs defined
 * in lightblue-client library for building projection JSON.
 *
 * <p>A static instance field of this type is readable for configuration generation. To generate an
 * the hook configuration JSON for an entity, define a static field on a class (usually the
 * notification implementation for that entity) which is an instance of this type. A builder is
 * provided via {@link #forEntity(String)}.
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

        private Projection watchProjection = Projection.project();
        private Projection includeProjection = Projection.project();
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
