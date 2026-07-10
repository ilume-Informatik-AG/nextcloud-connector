package de.ilume.nextcloud.outbound.model.action;

import io.camunda.connector.generator.java.annotation.TemplateDiscriminatorProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;

@TemplateDiscriminatorProperty(
        label = "Action",
        group = "action",
        name = "actionDiscriminator",
        defaultValue = NextcloudActionType.Constants.UPLOAD_FILE
)
@TemplateSubType(id = "action", label = "Action")
public sealed interface NextcloudAction permits
        CopyFileAction,
        CreateFolderAction,
        CreateNewShareAction,
        DeleteFileAction,
        DownloadFileAction,
        ListingFoldersAction,
        MoveFileAction,
        UploadFileAction
{

}
