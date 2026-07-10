package de.ilume.nextcloud.outbound.model.response;

import de.ilume.nextcloud.outbound.model.action.NextcloudActionType;
import io.camunda.connector.api.document.Document;

public record NextcloudDownloadResponse(
    NextcloudActionType actionType,
    String target,
    Document document
)
    implements NextcloudResponse { }
