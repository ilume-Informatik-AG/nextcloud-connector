package de.ilume.nextcloud.inbound.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;

// TODO: field set unverified against a real payload - confirmed shape (id, path) mirrors NodeCreatedEvent's
// documented example, only NodeCreatedEvent itself has been observed in Nextcloud's own docs so far.
public record NodeWrittenEvent(@JsonProperty("class") String eventClass, FileNode node)
        implements NextcloudFileEvent {
}
