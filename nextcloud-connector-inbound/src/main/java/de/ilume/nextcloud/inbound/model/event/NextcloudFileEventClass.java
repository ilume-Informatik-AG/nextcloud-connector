package de.ilume.nextcloud.inbound.model.event;

/**
 * Fully-qualified PHP event class names as sent by the Nextcloud "Webhook Listeners" app
 * (webhook_listeners) in the {@code event.class} field, e.g. {@code OCP\Files\Events\Node\NodeCreatedEvent}.
 * Only {@link #NODE_CREATED} matches a payload documented by Nextcloud itself; the others follow the
 * same {@code OCP\Files\Events\Node} family (see Nextcloud's admin manual) but their exact field set is
 * unconfirmed against a real payload so far.
 */
public class NextcloudFileEventClass {

    public static final String NODE_CREATED = "OCP\\Files\\Events\\Node\\NodeCreatedEvent";
    public static final String NODE_WRITTEN = "OCP\\Files\\Events\\Node\\NodeWrittenEvent";
    public static final String NODE_TOUCHED = "OCP\\Files\\Events\\Node\\NodeTouchedEvent";
    public static final String NODE_DELETED = "OCP\\Files\\Events\\Node\\NodeDeletedEvent";
    public static final String NODE_RENAMED = "OCP\\Files\\Events\\Node\\NodeRenamedEvent";
    public static final String NODE_COPIED = "OCP\\Files\\Events\\Node\\NodeCopiedEvent";

    private NextcloudFileEventClass() {
    }
}
