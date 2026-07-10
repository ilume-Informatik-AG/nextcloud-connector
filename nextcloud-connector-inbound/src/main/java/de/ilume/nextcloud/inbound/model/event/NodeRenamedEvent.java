package de.ilume.nextcloud.inbound.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;

// TODO: field set unverified against a real payload - two-node events are documented to carry
// "source"/"target" instead of a single "node" (see Nextcloud's webhook_listeners admin manual).
public record NodeRenamedEvent(@JsonProperty("class") String eventClass, FileNode source, FileNode target)
        implements NextcloudFileEvent {
}
