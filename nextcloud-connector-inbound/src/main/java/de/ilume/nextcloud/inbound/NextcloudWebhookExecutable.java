package de.ilume.nextcloud.inbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.ilume.nextcloud.inbound.model.NextcloudWebhookProperties;
import de.ilume.nextcloud.inbound.model.NextcloudWebhookProperties.NextcloudWebhookPropertiesWrapper;
import de.ilume.nextcloud.inbound.model.NextcloudWebhookResult;
import de.ilume.nextcloud.inbound.model.event.NextcloudFileEvent;
import de.ilume.nextcloud.inbound.model.event.NextcloudWebhookEnvelope;
import de.ilume.nextcloud.inbound.model.event.NodeCopiedEvent;
import de.ilume.nextcloud.inbound.model.event.NodeCreatedEvent;
import de.ilume.nextcloud.inbound.model.event.NodeDeletedEvent;
import de.ilume.nextcloud.inbound.model.event.NodeRenamedEvent;
import de.ilume.nextcloud.inbound.model.event.NodeTouchedEvent;
import de.ilume.nextcloud.inbound.model.event.NodeWrittenEvent;
import de.ilume.nextcloud.inbound.model.event.UnknownFileEvent;
import io.camunda.connector.api.annotation.InboundConnector;
import io.camunda.connector.api.inbound.Health;
import io.camunda.connector.api.inbound.InboundConnectorContext;
import io.camunda.connector.api.inbound.Severity;
import io.camunda.connector.api.inbound.webhook.MappedHttpRequest;
import io.camunda.connector.api.inbound.webhook.WebhookConnectorException;
import io.camunda.connector.api.inbound.webhook.WebhookConnectorException.WebhookSecurityException;
import io.camunda.connector.api.inbound.webhook.WebhookConnectorException.WebhookSecurityException.Reason;
import io.camunda.connector.api.inbound.webhook.WebhookConnectorExecutable;
import io.camunda.connector.api.inbound.webhook.WebhookHttpResponse;
import io.camunda.connector.api.inbound.webhook.WebhookProcessingPayload;
import io.camunda.connector.api.inbound.webhook.WebhookResult;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import io.camunda.connector.generator.java.annotation.ElementTemplate.PropertyGroup;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

@InboundConnector(name = "Nextcloud Webhook", type = "de.ilume:nextcloud-webhook:1")
@ElementTemplate(
        engineVersion = "^8.9",
        id = "de.ilume.connectors.nextcloud.webhook.v1",
        name = "Nextcloud Webhook Connector",
        description = "Receive Nextcloud file events (created, updated, deleted, renamed, copied, touched) via the "
                + "'Webhook Listeners' app",
        documentationRef = "https://tbd",
        icon = "ilume_logo.svg",
        version = 1,
        inputDataClass = NextcloudWebhookPropertiesWrapper.class,
        propertyGroups = {
                @PropertyGroup(id = "endpoint", label = "Webhook configuration"),
                @PropertyGroup(id = "authentication", label = "Authentication")
        },
        defaultResultExpression =
                "{\n"
                        + "  event: request.body.event.class\n"
                        + "  path: request.body.event.node.path\n"
                        + "  fileId: request.body.event.node.id\n"
                        + "  user: request.body.user.uid\n"
                        + "}")
public class NextcloudWebhookExecutable implements WebhookConnectorExecutable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private InboundConnectorContext context;
    private NextcloudWebhookProperties properties;

    @Override
    public void activate(InboundConnectorContext context) {
        this.context = context;
        this.properties = context.bindProperties(NextcloudWebhookPropertiesWrapper.class).inbound();
        context.reportHealth(Health.up());
    }

    @Override
    public WebhookHttpResponse verify(WebhookProcessingPayload payload) {
        return WebhookConnectorExecutable.super.verify(payload);
    }

    @Override
    public WebhookResult triggerWebhook(WebhookProcessingPayload payload) throws Exception {
        validateHttpMethod(payload);
        validateAuthHeader(payload);

        NextcloudWebhookEnvelope envelope = MAPPER.readValue(payload.rawBody(), NextcloudWebhookEnvelope.class);
        logEvent(envelope.event());

        // Parsed separately (rather than re-serializing `envelope`) so fields we don't model on
        // NextcloudFileEvent (e.g. additional node metadata) stay available to FEEL result/correlation expressions.
        Object body = MAPPER.readValue(payload.rawBody(), Object.class);
        var request = new MappedHttpRequest(body, payload.headers(), payload.params());
        return new NextcloudWebhookResult(request);
    }

    private void validateHttpMethod(WebhookProcessingPayload payload) {
        if (!"POST".equalsIgnoreCase(payload.method())) {
            throw new WebhookConnectorException(405, "Method " + payload.method() + " not supported");
        }
    }

    // Nextcloud's "Webhook Listeners" app (webhook_listeners) has no fixed signature scheme like older
    // third-party webhook apps - the admin picks an arbitrary header name/value pair per webhook via the
    // 'headers' field when registering it through the OCS API/occ. We just compare that header verbatim.
    private void validateAuthHeader(WebhookProcessingPayload payload) {
        String expected = properties.authHeaderValue();
        if (expected == null || expected.isBlank()) {
            return;
        }
        String headerName = properties.authHeaderName() == null || properties.authHeaderName().isBlank()
                ? NextcloudWebhookProperties.DEFAULT_AUTH_HEADER
                : properties.authHeaderName();
        String provided = payload.headers().get(headerName.toLowerCase(Locale.ROOT));
        if (provided == null || !MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
            throw new WebhookSecurityException(401, Reason.INVALID_CREDENTIALS);
        }
    }

    private void logEvent(NextcloudFileEvent event) {
        String tag;
        String path;
        switch (event) {
            case NodeCreatedEvent e -> {
                tag = "created";
                path = e.node().path();
            }
            case NodeWrittenEvent e -> {
                tag = "updated";
                path = e.node().path();
            }
            case NodeDeletedEvent e -> {
                tag = "deleted";
                path = e.node().path();
            }
            case NodeTouchedEvent e -> {
                tag = "touched";
                path = e.node().path();
            }
            case NodeRenamedEvent e -> {
                tag = "renamed";
                path = e.source().path() + " -> " + e.target().path();
            }
            case NodeCopiedEvent e -> {
                tag = "copied";
                path = e.source().path() + " -> " + e.target().path();
            }
            case UnknownFileEvent unknown -> {
                tag = "unknown (" + unknown.eventClass() + ")";
                path = "n/a";
            }
        }
        context.log(activity -> activity
                .withSeverity(Severity.INFO)
                .withTag(tag)
                .withMessage("Nextcloud file event: " + path));
    }

    @Override
    public void deactivate() {
        context.reportHealth(Health.down());
    }
}
