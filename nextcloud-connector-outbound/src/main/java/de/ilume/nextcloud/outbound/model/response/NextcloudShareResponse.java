package de.ilume.nextcloud.outbound.model.response;

import de.ilume.nextcloud.outbound.model.action.NextcloudActionType;

public record NextcloudShareResponse(
        NextcloudActionType actionType,
        String target,
        String shareId,
        String url,
        String token,
        String shareWith,
        String permissions,
        String expiration
) implements NextcloudResponse {}
