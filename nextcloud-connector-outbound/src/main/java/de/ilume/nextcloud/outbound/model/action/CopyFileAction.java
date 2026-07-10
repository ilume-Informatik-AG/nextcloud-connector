package de.ilume.nextcloud.outbound.model.action;

import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import jakarta.validation.constraints.NotBlank;

@TemplateSubType(id = "copyFile", label = "Copy File")
public record CopyFileAction(
    @TemplateProperty(
            label = "File Name",
            id = "copyActionFileName",
            binding = @TemplateProperty.PropertyBinding(name = "action.fileName"),
            group = "copyFile")
        @NotBlank
        String fileName,
    @TemplateProperty(
            label = "Source Path",
            id = "copyActionSourcePath",
            binding = @TemplateProperty.PropertyBinding(name = "action.sourcePath"),
            group = "copyFile")
        @NotBlank
        String sourcePath,
    @TemplateProperty(
            label = "Target Path",
            id = "copyActionTargetPath",
            binding = @TemplateProperty.PropertyBinding(name = "action.targetPath"),
            group = "copyFile")
        @NotBlank
        String targetPath)
    implements NextcloudAction {}
