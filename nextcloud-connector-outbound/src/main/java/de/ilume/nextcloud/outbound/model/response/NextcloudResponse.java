package de.ilume.nextcloud.outbound.model.response;

import de.ilume.nextcloud.outbound.model.action.NextcloudActionType;

public sealed interface NextcloudResponse
    permits NextcloudDownloadResponse,
        NextcloudFileOperationResponse,
        NextcloudDeleteResponse,
        NextcloudListResponse,
        NextcloudShareResponse {
  NextcloudActionType actionType();

  String target();
}
