package de.ilume.nextcloud.inbound.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// webhook_listeners only documents `id`/`path` on the node object so far - kept minimal and
// tolerant of extra fields (see NextcloudFileEventClass) rather than guessing a larger shape.
@JsonIgnoreProperties(ignoreUnknown = true)
public record FileNode(long id, String path) {
}
