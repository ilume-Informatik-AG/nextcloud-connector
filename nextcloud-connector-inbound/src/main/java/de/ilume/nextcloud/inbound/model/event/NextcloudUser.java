package de.ilume.nextcloud.inbound.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// null when the event was triggered without a logged-in user session (e.g. a public share upload).
@JsonIgnoreProperties(ignoreUnknown = true)
public record NextcloudUser(String uid, String displayName) {
}
