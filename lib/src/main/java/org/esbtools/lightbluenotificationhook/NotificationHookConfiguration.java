package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.metadata.parser.MetadataParser;
import com.redhat.lightblue.query.FieldProjection;
import com.redhat.lightblue.query.Projection;
import com.redhat.lightblue.util.Path;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Configures the notification hook for an entity.
 *
 * <p>watchProjection: This projection defines a part of the document
 * that is watched for changes. Any modifications to the fields
 * included in this projection will result in a notification being
 * fired
 *
 * <p>includeProjection: This projection defines the payload of the
 * message.
 *
 * <p>arrayOrderingSignificant: If this is set to true, and if the only
 * thing changed in an array is the ordering of its elements, an event
 * is still generated. If false, array re-orderings are
 * ignored. Default is false.
 */
public class NotificationHookConfiguration implements HookConfiguration {
    private static final Projection ALL_FIELDS = new FieldProjection(new Path("*"), true, true);
    private static final Projection NO_FIELDS = new FieldProjection(new Path("*"), false, false);

    private static final NotificationHookConfiguration WATCHING_EVERYTHING_INCLUDING_NOTHING
            = new NotificationHookConfiguration(ALL_FIELDS, NO_FIELDS, false);

    private final Projection watchProjection;
    private final Projection includeProjection;
    private final boolean arrayOrderingSignificant;

    /**
     * @param watchProjection If null, defaults to watching all fields.
     * @param includeProjection If null, defaults to including no fields.
     * @param arrayOrderingSignificant If an array has the same elements in different order, does
     * this count as a change?
     */
    public NotificationHookConfiguration(@Nullable Projection watchProjection,
                                         @Nullable Projection includeProjection,
                                         boolean arrayOrderingSignificant) {
        this.watchProjection = watchProjection != null ? watchProjection : ALL_FIELDS;
        this.includeProjection = includeProjection != null ? includeProjection : NO_FIELDS;
        this.arrayOrderingSignificant = arrayOrderingSignificant;
    }

    public static NotificationHookConfiguration watchingEverythingAndIncludingNothing() {
        return WATCHING_EVERYTHING_INCLUDING_NOTHING;
    }

    public static <T> NotificationHookConfiguration fromMetadata(MetadataParser<T> parser,
                                                                 @Nullable T parseMe) {
        if (parseMe == null) {
            return watchingEverythingAndIncludingNothing();
        }

        Projection watchProjection = parser.getProjection(parseMe, "watchProjection");
        Projection includeProjection = parser.getProjection(parseMe, "includeProjection");
        Object b=parser.getValueProperty(parseMe, "arrayOrderingSignificant");        
        
        return new NotificationHookConfiguration(watchProjection,
                                                 includeProjection,
                                                 b instanceof Boolean? (Boolean)b:false );
    }
    
    public Projection watchProjection() {
        return watchProjection;
    }
    
    public Projection includeProjection() {
        return includeProjection;
    }

    public boolean isArrayOrderingSignificant() {
        return arrayOrderingSignificant;
    }

    public <T> void toMetadata(MetadataParser<T> parser, T writeMe) {
        parser.putProjection(writeMe, "watchProjection", watchProjection);
        parser.putProjection(writeMe, "includeProjection", includeProjection);
        parser.putValue(writeMe,"arrayOrderingSignificant",Boolean.TRUE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NotificationHookConfiguration that = (NotificationHookConfiguration) o;
        return arrayOrderingSignificant == that.arrayOrderingSignificant &&
                Objects.equals(watchProjection, that.watchProjection) &&
                Objects.equals(includeProjection, that.includeProjection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(watchProjection, includeProjection, arrayOrderingSignificant);
    }

    @Override
    public String toString() {
        return "NotificationHookConfiguration{" +
                "watchProjection=" + watchProjection +
                ", includeProjection=" + includeProjection +
                ", arrayOrderingSignificant=" + arrayOrderingSignificant +
                '}';
    }
}
