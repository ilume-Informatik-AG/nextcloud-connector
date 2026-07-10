package de.ilume.nextcloud.inbound.model;

import io.camunda.connector.api.inbound.webhook.MappedHttpRequest;
import io.camunda.connector.api.inbound.webhook.WebhookResult;

public record NextcloudWebhookResult(MappedHttpRequest request) implements WebhookResult {
}
