package de.ilume.nextcloud.inbound.model.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Exercises the {@code event.class}-discriminated Jackson polymorphism ({@link NextcloudFileEvent})
 * against the payload shapes documented in {@code nextcloud-connector-inbound/README.md} - the
 * discriminator values ({@link NextcloudFileEventClass}) and per-type field names are hand-typed in
 * multiple places (subtype annotations, record fields), so a typo there would silently misroute an
 * event (or break deserialization) without any compiler help.
 */
class NextcloudFileEventTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @ParameterizedTest
  @MethodSource("singleNodeEventClasses")
  void deserializesSingleNodeEvents(String eventClassName, Class<? extends NextcloudFileEvent> expectedType)
      throws Exception {
    String json = MAPPER.writeValueAsString(eventNode(eventClassName, 437, "/admin/files/test-webhook.txt"));

    NextcloudFileEvent event = MAPPER.readValue(json, NextcloudFileEvent.class);

    assertThat(event).isInstanceOf(expectedType);
    assertThat(event.eventClass()).isEqualTo(eventClassName);
  }

  private static Stream<Arguments> singleNodeEventClasses() {
    return Stream.of(
        Arguments.of(NextcloudFileEventClass.NODE_CREATED, NodeCreatedEvent.class),
        Arguments.of(NextcloudFileEventClass.NODE_WRITTEN, NodeWrittenEvent.class),
        Arguments.of(NextcloudFileEventClass.NODE_TOUCHED, NodeTouchedEvent.class),
        Arguments.of(NextcloudFileEventClass.NODE_DELETED, NodeDeletedEvent.class));
  }

  @Test
  void deserializesNodeCreatedEvent_nodeFieldsPopulated() throws Exception {
    String json = MAPPER.writeValueAsString(
        eventNode(NextcloudFileEventClass.NODE_CREATED, 437, "/admin/files/test-webhook.txt"));

    NodeCreatedEvent event = (NodeCreatedEvent) MAPPER.readValue(json, NextcloudFileEvent.class);

    assertThat(event.node().id()).isEqualTo(437);
    assertThat(event.node().path()).isEqualTo("/admin/files/test-webhook.txt");
  }

  @ParameterizedTest
  @MethodSource("sourceTargetEventClasses")
  void deserializesSourceTargetEvents(String eventClassName, Class<? extends NextcloudFileEvent> expectedType)
      throws Exception {
    String json = MAPPER.writeValueAsString(sourceTargetNode(eventClassName));

    NextcloudFileEvent event = MAPPER.readValue(json, NextcloudFileEvent.class);

    assertThat(event).isInstanceOf(expectedType);
  }

  private static Stream<Arguments> sourceTargetEventClasses() {
    return Stream.of(
        Arguments.of(NextcloudFileEventClass.NODE_RENAMED, NodeRenamedEvent.class),
        Arguments.of(NextcloudFileEventClass.NODE_COPIED, NodeCopiedEvent.class));
  }

  @Test
  void deserializesNodeRenamedEvent_sourceAndTargetPopulated() throws Exception {
    String json = MAPPER.writeValueAsString(sourceTargetNode(NextcloudFileEventClass.NODE_RENAMED));

    NodeRenamedEvent event = (NodeRenamedEvent) MAPPER.readValue(json, NextcloudFileEvent.class);

    assertThat(event.source().path()).isEqualTo("/admin/files/old-name.txt");
    assertThat(event.target().path()).isEqualTo("/admin/files/new-name.txt");
  }

  @Test
  void unrecognizedEventClass_fallsBackToUnknownFileEvent() throws Exception {
    String eventClassName = "OCP\\Files\\Events\\Node\\NodeRestoredEvent";
    String json = MAPPER.writeValueAsString(Map.of("class", eventClassName));

    NextcloudFileEvent event = MAPPER.readValue(json, NextcloudFileEvent.class);

    assertThat(event).isInstanceOf(UnknownFileEvent.class);
    assertThat(event.eventClass()).isEqualTo(eventClassName);
  }

  @Test
  void envelope_deserializesEventUserAndTime_ignoringUnknownFields() throws Exception {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put(
        "event", eventNode(NextcloudFileEventClass.NODE_CREATED, 437, "/admin/files/test-webhook.txt"));
    payload.put("user", Map.of("uid", "admin", "displayName", "Admin"));
    payload.put("time", 1700100000);
    payload.put("someFutureField", "should be ignored, not fail deserialization");

    NextcloudWebhookEnvelope envelope =
        MAPPER.readValue(MAPPER.writeValueAsString(payload), NextcloudWebhookEnvelope.class);

    assertThat(envelope.event()).isInstanceOf(NodeCreatedEvent.class);
    assertThat(envelope.user().uid()).isEqualTo("admin");
    assertThat(envelope.time()).isEqualTo(1700100000L);
  }

  @Test
  void envelope_toleratesMissingUser_forPublicShareUploads() throws Exception {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("event", eventNode(NextcloudFileEventClass.NODE_CREATED, 1, "/public/upload.txt"));
    payload.put("time", 1700100000);

    NextcloudWebhookEnvelope envelope =
        MAPPER.readValue(MAPPER.writeValueAsString(payload), NextcloudWebhookEnvelope.class);

    assertThat(envelope.user()).isNull();
  }

  private static Map<String, Object> eventNode(String eventClassName, long nodeId, String nodePath) {
    Map<String, Object> node = new LinkedHashMap<>();
    node.put("class", eventClassName);
    node.put("node", Map.of("id", nodeId, "path", nodePath));
    return node;
  }

  private static Map<String, Object> sourceTargetNode(String eventClassName) {
    Map<String, Object> node = new LinkedHashMap<>();
    node.put("class", eventClassName);
    node.put("source", Map.of("id", 1, "path", "/admin/files/old-name.txt"));
    node.put("target", Map.of("id", 1, "path", "/admin/files/new-name.txt"));
    return node;
  }
}
