package de.ilume.nextcloud.outbound.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.ilume.nextcloud.outbound.model.action.*;
import io.camunda.connector.generator.java.annotation.NestedProperties;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record NextcloudRequest(
    @TemplateProperty(group = "authentication", id = "type") @Valid @NotNull
        NextcloudAuthentication authentication,
    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
            property = "actionDiscriminator")
        @JsonSubTypes(
            value = {
              @JsonSubTypes.Type(value = CopyFileAction.class, name = NextcloudActionType.Constants.COPY_FILE),
              @JsonSubTypes.Type(value = CreateFolderAction.class, name = NextcloudActionType.Constants.CREATE_FOLDER),
              @JsonSubTypes.Type(value = CreateNewShareAction.class, name = NextcloudActionType.Constants.CREATE_NEW_SHARE),
              @JsonSubTypes.Type(value = DeleteFileAction.class, name = NextcloudActionType.Constants.DELETE_FILE),
              @JsonSubTypes.Type(value = DownloadFileAction.class, name = NextcloudActionType.Constants.DOWNLOAD_FILE),
              @JsonSubTypes.Type(value = ListingFoldersAction.class, name = NextcloudActionType.Constants.LISTING_FOLDERS),
              @JsonSubTypes.Type(value = MoveFileAction.class, name = NextcloudActionType.Constants.MOVE_FILE),
              @JsonSubTypes.Type(value = UploadFileAction.class, name = NextcloudActionType.Constants.UPLOAD_FILE),
            })
        @Valid
        @NotNull
        @NestedProperties(addNestedPath = false)
        NextcloudAction action

    /*
    @TemplateProperty(group = "configuration")
    private NextcloudConfiguration
     */
    ) {}
