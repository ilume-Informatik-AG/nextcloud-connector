package de.ilume.nextcloud.inbound.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Fallback for any {@code event.class} not in {@link NextcloudFileEventClass} (e.g. a "Before*"
 * variant, {@code NodeRestoredEvent}, or a Nextcloud version sending an event we don't model yet),
 * so unrecognized events don't fail deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UnknownFileEvent(@JsonProperty("class") String eventClass) implements NextcloudFileEvent {
}
