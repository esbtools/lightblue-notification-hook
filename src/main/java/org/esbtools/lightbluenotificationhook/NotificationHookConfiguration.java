package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.metadata.parser.MetadataParser;
import com.redhat.lightblue.query.Projection;

import java.util.Objects;

public class NotificationHookConfiguration implements HookConfiguration {
    private final Projection watchProjection;
    private final Projection includeProjection;

    public NotificationHookConfiguration(Projection watchProjection, Projection includeProjection) {
        this.watchProjection = watchProjection;
        this.includeProjection = includeProjection;
    }

    public static NotificationHookConfiguration fromMetadata(MetadataParser<Object> parser,
            Object parseMe) {
        Projection watchProjection = parser.getProjection(parseMe, "watchProjection");
        Projection includeProjection = parser.getProjection(parseMe, "includeProjection");

        return new NotificationHookConfiguration(watchProjection, includeProjection);
    }

    public Projection watchProjection() {
        return watchProjection;
    }

    public Projection includeProjection() {
        return includeProjection;
    }

    public void toMetadata(MetadataParser<Object> parser, Object writeMe) {
        parser.putProjection(writeMe, "watchProjection", watchProjection);
        parser.putProjection(writeMe, "includeProjection", includeProjection);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotificationHookConfiguration that = (NotificationHookConfiguration) o;
        return Objects.equals(watchProjection, that.watchProjection) &&
                Objects.equals(includeProjection, that.includeProjection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(watchProjection, includeProjection);
    }

    @Override
    public String toString() {
        return "NotificationHookConfiguration{" +
                "watchProjection=" + watchProjection +
                ", includeProjection=" + includeProjection +
                '}';
    }
}
