package de.ilume.nextcloud.outbound.model.action;

import de.ilume.nextcloud.outbound.model.MetadataSelection;
import de.ilume.nextcloud.outbound.utils.PathUtil;
import io.camunda.connector.generator.java.annotation.FeelMode;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@TemplateSubType(id = "listingFolders", label = "List Folder Contents")
public record ListingFoldersAction(
        @TemplateProperty(
                label = "Folder Path",
                group = "listingFolders",
                tooltip = "The path to the folder. e.g. '/documents'. Use '/' for root",
                feel = FeelMode.optional,
                id = "listingFoldersPath",
                binding = @TemplateProperty.PropertyBinding(name = "action.path"))
        @NotBlank
        String path,
        @TemplateProperty(
                label = "Search Depth",
                type = TemplateProperty.PropertyType.Number,
                defaultValue = "1",
                defaultValueType = TemplateProperty.DefaultValueType.Number,
                description = "Depth 1 shows files and folders inside. Depth 0 shows only the folder's own metadata.",
                group = "listingFolders",
                id = "listingFoldersDepth",
                binding = @TemplateProperty.PropertyBinding(name = "action.depth"))
        @NotNull
        Integer depth,
        @TemplateProperty(
                label = "Metadata Selection",
                type = TemplateProperty.PropertyType.Dropdown,
                id = "listingFoldersMetadataSelection",
                group = "listingFolders",
                defaultValue = "default",
                binding = @TemplateProperty.PropertyBinding(name = "action.metadataSelection"))
        @NotNull
        MetadataSelection metadataSelection,
        @TemplateProperty(
                label = "Additional Properties",
                tooltip = "Use a map<Namespace, Property-Name> with additional properties that should be included in the result",
                feel = FeelMode.required,
                id = "listingFoldersAdditionalProperties",
                group = "listingFolders",
                binding = @TemplateProperty.PropertyBinding(name = "action.additionalProperties"),
                condition = @TemplateProperty.PropertyCondition(
                        property = "action.metadataSelection",
                        equals = "custom"
                ))
        Map<String, String> additionalProperties
) implements NextcloudAction {

    @AssertTrue(message = "'additionalProperties' are required when metadata selection is custom.")
    public boolean isAdditionalPropertiesValid() {
        if (metadataSelection == MetadataSelection.CUSTOM) {
            return additionalProperties != null && !additionalProperties.isEmpty();
        }
        return true;
    }


  public ListingFoldersAction {
    // Fallback for default values
    if (depth == null) {
      depth = 1;
    }

    path = PathUtil.normalizePath(path);
  }
}
