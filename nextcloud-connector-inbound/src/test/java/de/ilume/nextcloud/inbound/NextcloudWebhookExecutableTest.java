package de.ilume.nextcloud.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.ilume.nextcloud.inbound.model.NextcloudWebhookProperties;
import de.ilume.nextcloud.inbound.model.NextcloudWebhookProperties.NextcloudWebhookPropertiesWrapper;
import io.camunda.connector.api.inbound.InboundConnectorContext;
import io.camunda.connector.api.inbound.webhook.Part;
import io.camunda.connector.api.inbound.webhook.WebhookConnectorException;
import io.camunda.connector.api.inbound.webhook.WebhookConnectorException.WebhookSecurityException;
import io.camunda.connector.api.inbound.webhook.WebhookProcessingPayload;
import io.camunda.connector.api.inbound.webhook.WebhookResult;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link NextcloudWebhookExecutable#triggerWebhook}'s two hand-rolled validation steps -
 * HTTP method and auth-header checks - since webhook_listeners has no fixed signature scheme (see
 * the comment on {@code validateAuthHeader}), so a mistake here directly means the connector either
 * accepts forged requests or rejects legitimate ones.
 */
class NextcloudWebhookExecutableTest {

  private static final String NODE_CREATED_BODY =
      """
      {
        "event": {
          "class": "OCP\\\\Files\\\\Events\\\\Node\\\\NodeCreatedEvent",
          "node": { "id": 437, "path": "/admin/files/test-webhook.txt" }
        },
        "user": { "uid": "admin", "displayName": "Admin" },
        "time": 1700100000
      }
      """;

  private NextcloudWebhookExecutable executable;
  private InboundConnectorContext context;

  @BeforeEach
  void setUp() {
    executable = new NextcloudWebhookExecutable();
    context = mock(InboundConnectorContext.class);
  }

  private void activateWith(String authHeaderName, String authHeaderValue) {
    var properties = new NextcloudWebhookProperties("my-webhook-id", authHeaderName, authHeaderValue);
    when(context.bindProperties(NextcloudWebhookPropertiesWrapper.class))
        .thenReturn(new NextcloudWebhookPropertiesWrapper(properties));
    executable.activate(context);
  }

  @Test
  void triggerWebhook_rejectsNonPostMethod() {
    activateWith(null, null);
    var payload = payload("GET", Map.of());

    assertThatThrownBy(() -> executable.triggerWebhook(payload))
        .isInstanceOf(WebhookConnectorException.class)
        .extracting(e -> ((WebhookConnectorException) e).getStatusCode())
        .isEqualTo(405);
  }

  @Test
  void triggerWebhook_noExpectedHeaderConfigured_acceptsRequestWithoutAnyHeader() throws Exception {
    activateWith(null, null);
    var payload = payload("POST", Map.of());

    WebhookResult result = executable.triggerWebhook(payload);

    assertThat(result.request().body()).isNotNull();
  }

  @Test
  void triggerWebhook_correctDefaultAuthHeader_succeeds() throws Exception {
    activateWith(null, "s3cr3t");
    var payload = payload("POST", Map.of("x-webhook-secret", "s3cr3t"));

    WebhookResult result = executable.triggerWebhook(payload);

    assertThat(result.request().body()).isNotNull();
  }

  @Test
  void triggerWebhook_missingAuthHeader_throwsSecurityException401() {
    activateWith(null, "s3cr3t");
    var payload = payload("POST", Map.of());

    assertThatThrownBy(() -> executable.triggerWebhook(payload))
        .isInstanceOf(WebhookSecurityException.class)
        .extracting(e -> ((WebhookConnectorException) e).getStatusCode())
        .isEqualTo(401);
  }

  @Test
  void triggerWebhook_wrongAuthHeaderValue_throwsSecurityException401() {
    activateWith(null, "s3cr3t");
    var payload = payload("POST", Map.of("x-webhook-secret", "wrong-value"));

    assertThatThrownBy(() -> executable.triggerWebhook(payload))
        .isInstanceOf(WebhookSecurityException.class)
        .extracting(e -> ((WebhookConnectorException) e).getStatusCode())
        .isEqualTo(401);
  }

  @Test
  void triggerWebhook_customAuthHeaderName_isRespected() throws Exception {
    activateWith("X-My-Secret", "s3cr3t");
    var payload = payload("POST", Map.of("x-my-secret", "s3cr3t"));

    WebhookResult result = executable.triggerWebhook(payload);

    assertThat(result.request().body()).isNotNull();
  }

  @Test
  void triggerWebhook_customAuthHeaderName_defaultHeaderNameNoLongerAccepted() {
    activateWith("X-My-Secret", "s3cr3t");
    // sent under the default header name instead of the configured custom one
    var payload = payload("POST", Map.of("x-webhook-secret", "s3cr3t"));

    assertThatThrownBy(() -> executable.triggerWebhook(payload))
        .isInstanceOf(WebhookSecurityException.class);
  }

  @Test
  void triggerWebhook_unrecognizedEventClass_stillReturnsResultInsteadOfFailing() throws Exception {
    activateWith(null, null);
    String body =
        """
        {
          "event": { "class": "OCP\\\\Files\\\\Events\\\\Node\\\\NodeRestoredEvent" },
          "user": { "uid": "admin", "displayName": "Admin" },
          "time": 1700100000
        }
        """;

    WebhookResult result = executable.triggerWebhook(payload("POST", Map.of(), body));

    assertThat(result.request().body()).isNotNull();
  }

  private static WebhookProcessingPayload payload(String method, Map<String, String> headers) {
    return payload(method, headers, NODE_CREATED_BODY);
  }

  private static WebhookProcessingPayload payload(
      String method, Map<String, String> headers, String rawBody) {
    return new WebhookProcessingPayload() {
      @Override
      public String requestURL() {
        return "https://runtime-host/inbound/my-webhook-id";
      }

      @Override
      public String method() {
        return method;
      }

      @Override
      public Map<String, String> headers() {
        return headers;
      }

      @Override
      public Map<String, String> params() {
        return Map.of();
      }

      @Override
      public byte[] rawBody() {
        return rawBody.getBytes(StandardCharsets.UTF_8);
      }

      @Override
      public Collection<Part> parts() {
        return List.of();
      }
    };
  }
}
