package org.esbtools.lightbluenotificationhook;

import com.redhat.lightblue.hooks.CRUDHook;
import com.redhat.lightblue.metadata.HookConfiguration;
import com.redhat.lightblue.metadata.parser.HookConfigurationParser;
import com.redhat.lightblue.metadata.parser.MetadataParser;

/**
 * Entry-point for lightblue to the hook.
 */
public class NotificationHookConfigurationParser<T> implements HookConfigurationParser<T> {
    @Override
    public String getName() {
        return "notificationHook";
    }

    @Override
    public CRUDHook getCRUDHook() {
        return new NotificationHook(getName());
    }

    @Override
    public NotificationHookConfiguration parse(String name, MetadataParser<T> parser, T parseMe) {
        return NotificationHookConfiguration.fromMetadata(parser, parseMe);
    }

    @Override
    public void convert(MetadataParser<T> parser, T writeMe, HookConfiguration hookConfiguration) {
        if (hookConfiguration == null) {
            hookConfiguration = NotificationHookConfiguration.watchingEverythingAndIncludingNothing();
        } else if (!(hookConfiguration instanceof NotificationHookConfiguration)) {
            throw new IllegalArgumentException("Can only parse NotificationHookConfiguration but " +
                    "got: " + hookConfiguration);
        }

        NotificationHookConfiguration config = (NotificationHookConfiguration) hookConfiguration;

        config.toMetadata(parser, writeMe);
    }
}
