package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.metadata.parser.MetadataParser;
import com.redhat.lightblue.query.Projection;

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
    private final Projection watchProjection;
    private final Projection includeProjection;
    private final boolean arrayOrderingSignificant;

    public NotificationHookConfiguration(Projection watchProjection,
                                         Projection includeProjection,
                                         boolean arrayOrderingSignificant) {
        this.watchProjection = watchProjection;
        this.includeProjection = includeProjection;
        this.arrayOrderingSignificant = arrayOrderingSignificant;
    }

    public static <T> NotificationHookConfiguration fromMetadata(MetadataParser<T> parser,
                                                                 T parseMe) {
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
        if(arrayOrderingSignificant)
            parser.putValue(writeMe,"arrayOrderingSignificant",Boolean.TRUE);
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
