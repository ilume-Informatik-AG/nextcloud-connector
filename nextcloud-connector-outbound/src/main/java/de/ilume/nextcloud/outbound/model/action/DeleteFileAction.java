package de.ilume.nextcloud.outbound.model.action;

import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import jakarta.validation.constraints.NotBlank;

@TemplateSubType(id = "deleteFile", label = "Delete File")
public record DeleteFileAction(
    @TemplateProperty(
            label = "Target Path",
            id = "deleteFilePath",
            binding = @TemplateProperty.PropertyBinding(name = "action.path"),
            group = "deleteFile")
        @NotBlank
        String path,
    @TemplateProperty(
            label = "File Name",
            id = "deleteFileName",
            binding = @TemplateProperty.PropertyBinding(name = "action.fileName"),
            group = "deleteFile")
        @NotBlank
        String fileName)
    implements NextcloudAction {}
