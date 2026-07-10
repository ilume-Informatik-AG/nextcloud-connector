package de.ilume.nextcloud.outbound.model.response;

import de.ilume.nextcloud.outbound.model.action.NextcloudActionType;
import java.util.List;

public record NextcloudListResponse(
        NextcloudActionType actionType,
        String target,
        List<NextcloudResourceDto> resources

) implements NextcloudResponse {}
