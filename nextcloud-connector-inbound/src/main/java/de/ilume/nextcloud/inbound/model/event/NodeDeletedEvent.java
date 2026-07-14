package de.ilume.nextcloud.inbound.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NodeDeletedEvent(@JsonProperty("class") String eventClass, FileNode node)
        implements NextcloudFileEvent {
}
