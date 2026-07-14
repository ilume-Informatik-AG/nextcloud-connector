package de.ilume.nextcloud.inbound.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NodeRenamedEvent(@JsonProperty("class") String eventClass, FileNode source, FileNode target)
        implements NextcloudFileEvent {
}
