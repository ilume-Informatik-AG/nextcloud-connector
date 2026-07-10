package de.ilume.nextcloud.outbound;

import de.ilume.nextcloud.outbound.core.NextcloudExecutor;
import de.ilume.nextcloud.outbound.model.NextcloudRequest;
import de.ilume.nextcloud.outbound.model.action.NextcloudActionType;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.document.Document;
import io.camunda.connector.api.document.DocumentCreationRequest;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.generator.java.annotation.ElementTemplate;

import java.util.function.Function;

@OutboundConnector(
        name = "Nextcloud Connector Outbound",
        inputVariables = {"authentication", "action", "actionDiscriminator"},
        type = "de.ilume:nextcloud-outbound:1")
@ElementTemplate(
        engineVersion = "^8.9",
        id = "de.ilume.connectors.nextcloud.outbound.v1",
        name = "Nextcloud Outbound Connector",
        description = "Execute Nextcloud operations",
        inputDataClass = NextcloudRequest.class,
        version = 1,
        propertyGroups = {
                @ElementTemplate.PropertyGroup(id = "authentication", label = "Authentication"),
                //@ElementTemplate.PropertyGroup(id = "configuration", label = "Configuration"),
                @ElementTemplate.PropertyGroup(id = "action", label = "Action"),
                @ElementTemplate.PropertyGroup(id = NextcloudActionType.Constants.COPY_FILE, label = "Copy File"),
                @ElementTemplate.PropertyGroup(id = NextcloudActionType.Constants.CREATE_FOLDER, label = "Create Folder"),
                @ElementTemplate.PropertyGroup(id = NextcloudActionType.Constants.CREATE_NEW_SHARE, label = "Create New Share"),
                @ElementTemplate.PropertyGroup(id = NextcloudActionType.Constants.DELETE_FILE, label = "Delete File"),
                @ElementTemplate.PropertyGroup(id = NextcloudActionType.Constants.DOWNLOAD_FILE, label = "Download File"),
                @ElementTemplate.PropertyGroup(id = NextcloudActionType.Constants.LISTING_FOLDERS, label = "List Folder Contents"),
                @ElementTemplate.PropertyGroup(id = NextcloudActionType.Constants.MOVE_FILE, label = "Move File"),
                @ElementTemplate.PropertyGroup(id = NextcloudActionType.Constants.UPLOAD_FILE, label = "Upload File"),
        },
        documentationRef = "https://tbd",
        icon = "ilume_logo.svg")
public class NextcloudConnectorFunction implements OutboundConnectorFunction {

    @Override
    public Object execute(OutboundConnectorContext context) {
        Function<DocumentCreationRequest, Document> createDocument = context::create;
        NextcloudRequest nRequest = context.bindVariables(NextcloudRequest.class);
        return NextcloudExecutor.create(nRequest, createDocument).execute(nRequest.action());
    }
}
