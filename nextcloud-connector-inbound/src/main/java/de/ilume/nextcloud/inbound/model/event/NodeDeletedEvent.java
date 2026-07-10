package de.ilume.nextcloud.inbound.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;

// TODO: field set unverified against a real payload, see NodeWrittenEvent.
public record NodeDeletedEvent(@JsonProperty("class") String eventClass, FileNode node)
        implements NextcloudFileEvent {
}
