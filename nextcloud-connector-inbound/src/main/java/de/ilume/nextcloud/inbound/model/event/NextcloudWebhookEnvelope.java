package de.ilume.nextcloud.inbound.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Top-level payload envelope sent by Nextcloud's "Webhook Listeners" app (webhook_listeners) for
// every webhook call, regardless of event type: {"event": {...}, "user": {...}, "time": ...}.
@JsonIgnoreProperties(ignoreUnknown = true)
public record NextcloudWebhookEnvelope(NextcloudFileEvent event, NextcloudUser user, long time) {
}
