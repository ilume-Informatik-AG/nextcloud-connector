package de.ilume.nextcloud.outbound.model.action;

import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import jakarta.validation.constraints.NotBlank;

@TemplateSubType(id = "downloadFile", label = "Download File")
public record DownloadFileAction(
    @TemplateProperty(
            label = "Target Path",
            id = "downloadFilePath",
            binding = @TemplateProperty.PropertyBinding(name = "action.path"),
            group = "downloadFile")
        @NotBlank
        String path,
    @TemplateProperty(
            label = "File Name",
            id = "downloadFileName",
            binding = @TemplateProperty.PropertyBinding(name = "action.fileName"),
            group = "downloadFile")
        @NotBlank
        String fileName)
    implements NextcloudAction {}
