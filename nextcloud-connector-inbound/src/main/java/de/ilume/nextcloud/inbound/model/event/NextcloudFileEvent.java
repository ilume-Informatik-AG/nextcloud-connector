package de.ilume.nextcloud.inbound.model.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "class",
        visible = true,
        defaultImpl = UnknownFileEvent.class
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = NodeCreatedEvent.class, name = NextcloudFileEventClass.NODE_CREATED),
        @JsonSubTypes.Type(value = NodeWrittenEvent.class, name = NextcloudFileEventClass.NODE_WRITTEN),
        @JsonSubTypes.Type(value = NodeTouchedEvent.class, name = NextcloudFileEventClass.NODE_TOUCHED),
        @JsonSubTypes.Type(value = NodeDeletedEvent.class, name = NextcloudFileEventClass.NODE_DELETED),
        @JsonSubTypes.Type(value = NodeRenamedEvent.class, name = NextcloudFileEventClass.NODE_RENAMED),
        @JsonSubTypes.Type(value = NodeCopiedEvent.class, name = NextcloudFileEventClass.NODE_COPIED)
})
public sealed interface NextcloudFileEvent
        permits NodeCreatedEvent, NodeWrittenEvent, NodeTouchedEvent, NodeDeletedEvent, NodeRenamedEvent,
        NodeCopiedEvent, UnknownFileEvent {

    String eventClass();
}
