package de.ilume.nextcloud.outbound.model.response;

import de.ilume.nextcloud.outbound.model.action.NextcloudActionType;

public record NextcloudDeleteResponse(
        NextcloudActionType actionType,
        String target
) implements NextcloudResponse { }
