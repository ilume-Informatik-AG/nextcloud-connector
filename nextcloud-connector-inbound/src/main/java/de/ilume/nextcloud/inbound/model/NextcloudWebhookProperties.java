package de.ilume.nextcloud.inbound.model;

import io.camunda.connector.generator.java.annotation.FeelMode;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NextcloudWebhookProperties(
        @TemplateProperty(
                id = "context",
                label = "Webhook ID",
                group = "endpoint",
                description = "Part of the inbound URL, e.g. https://runtime-host/inbound/<webhook-id>",
                feel = FeelMode.disabled)
        @NotBlank
        @Pattern(
                regexp = "^[a-zA-Z0-9]+([-_][a-zA-Z0-9]+)*$",
                message = "can only contain letters, numbers, or single underscores/hyphens and cannot begin or end with an underscore/hyphen")
        String context,

        @TemplateProperty(
                id = "authHeaderName",
                label = "Auth header name",
                group = "authentication",
                description = "Name of the custom HTTP header configured in the 'headers' field when the webhook "
                        + "was registered with Nextcloud's 'Webhook Listeners' app (webhook_listeners). Leave the "
                        + "value below empty to disable this check.",
                optional = true,
                feel = FeelMode.disabled,
                defaultValue = NextcloudWebhookProperties.DEFAULT_AUTH_HEADER)
        String authHeaderName,

        @TemplateProperty(
                id = "authHeaderValue",
                label = "Expected header value",
                group = "authentication",
                description = "Expected value of the auth header above, must match exactly what was configured for "
                        + "this webhook in Nextcloud. Leave empty to disable this check (not recommended).",
                optional = true,
                feel = FeelMode.optional)
        String authHeaderValue
) {

    // Not fixed by Nextcloud - webhook_listeners lets the admin pick any header name/value pair per webhook
    // at registration time (OCS API/occ), so this is only a suggested default, not a protocol requirement.
    public static final String DEFAULT_AUTH_HEADER = "X-Webhook-Secret";

    public record NextcloudWebhookPropertiesWrapper(NextcloudWebhookProperties inbound) {
    }
}
