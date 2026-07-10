package de.ilume.nextcloud.outbound.model.action;

import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import jakarta.validation.constraints.NotBlank;

@TemplateSubType(id = "createFolder", label = "Create Folder")
public record CreateFolderAction(
        @TemplateProperty(
            label = "Target Path",
            id = "createFolderPath",
            binding = @TemplateProperty.PropertyBinding(name = "action.path"),
            group = "createFolder")
    @NotBlank
    String path,
        @TemplateProperty(
                label = "Folder Name",
                id = "createFolderFolderName",
                binding = @TemplateProperty.PropertyBinding(name = "action.folderName"),
                group = "createFolder")
    @NotBlank
    String folderName)
    implements NextcloudAction {}
