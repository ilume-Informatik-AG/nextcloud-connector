package de.ilume.nextcloud.outbound.model.action;

import io.camunda.connector.api.document.Document;
import io.camunda.connector.generator.java.annotation.FeelMode;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@TemplateSubType(id = "uploadFile", label = "Upload File")
public record UploadFileAction(
    @TemplateProperty(
            label = "Target Path",
            id = "uploadFilePath",
            binding = @TemplateProperty.PropertyBinding(name = "action.path"),
            group = "uploadFile")
        @NotEmpty
        String path,
    @TemplateProperty(
            label = "Document",
            id = "uploadFileDocument",
            binding = @TemplateProperty.PropertyBinding(name = "action.document"),
            type = TemplateProperty.PropertyType.String,
            group = "uploadFile",
            feel = FeelMode.required)
        @NotNull
        Document document)
    implements NextcloudAction {}
