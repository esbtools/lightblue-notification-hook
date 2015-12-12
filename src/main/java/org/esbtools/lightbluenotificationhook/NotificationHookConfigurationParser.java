package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.hooks.CRUDHook;
import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.metadata.parser.HookConfigurationParser;
import com.redhat.lightblue.metadata.parser.MetadataParser;

/**
 * Entry-point for lightblue to the hook.
 *
 * <p>The generic type indicates the underlying metadata serialization format. As generics are not
 * enforced at runtime, we don't care what it is.
 */
public class NotificationHookConfigurationParser implements HookConfigurationParser<Object> {
    @Override
    public String getName() {
        return "notificationHook";
    }

    @Override
    public CRUDHook getCRUDHook() {
        return new NotificationHook(getName());
    }

    @Override
    public NotificationHookConfiguration parse(String name, MetadataParser<Object> parser,
            Object parseMe) {
        return NotificationHookConfiguration.fromMetadata(parser, parseMe);
    }

    @Override
    public void convert(MetadataParser<Object> parser, Object writeMe,
            HookConfiguration hookConfiguration) {
        if (!(hookConfiguration instanceof NotificationHookConfiguration)) {
            throw new IllegalArgumentException("Can only parse NotificationHookConfiguration but " +
                    "got: " + hookConfiguration);
        }

        NotificationHookConfiguration config = (NotificationHookConfiguration) hookConfiguration;

        config.toMetadata(parser, writeMe);
    }
}
