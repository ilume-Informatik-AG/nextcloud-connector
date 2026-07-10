package de.ilume.nextcloud.outbound.model.response;

import de.ilume.nextcloud.outbound.model.action.NextcloudActionType;

public record NextcloudFileOperationResponse(
        NextcloudActionType actionType,
        String source,
        String target,
        String fileName
) implements NextcloudResponse {}
