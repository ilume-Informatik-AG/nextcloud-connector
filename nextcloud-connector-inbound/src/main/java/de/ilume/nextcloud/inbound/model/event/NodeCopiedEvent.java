package de.ilume.nextcloud.inbound.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;

// TODO: field set unverified against a real payload, see NodeRenamedEvent.
public record NodeCopiedEvent(@JsonProperty("class") String eventClass, FileNode source, FileNode target)
        implements NextcloudFileEvent {
}
