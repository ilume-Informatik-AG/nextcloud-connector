package de.ilume.nextcloud.outbound.model.action;

import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import jakarta.validation.constraints.NotBlank;

@TemplateSubType(id = "moveFile", label = "Move File")
public record MoveFileAction(
    @TemplateProperty(
            label = "File Name",
            id = "moveFileFileName",
            group = "moveFile",
            binding = @TemplateProperty.PropertyBinding(name = "action.fileName"))
        @NotBlank
        String fileName,
    @TemplateProperty(
            label = "Source Path",
            id = "moveFileSourcePath",
            group = "moveFile",
            binding = @TemplateProperty.PropertyBinding(name = "action.sourcePath"))
        @NotBlank
        String sourcePath,
    @TemplateProperty(
            label = "Target Path",
            id = "moveFileTargetPath",
            group = "moveFile",
            binding = @TemplateProperty.PropertyBinding(name = "action.targetPath"))
        @NotBlank
        String targetPath)
    implements NextcloudAction {}
