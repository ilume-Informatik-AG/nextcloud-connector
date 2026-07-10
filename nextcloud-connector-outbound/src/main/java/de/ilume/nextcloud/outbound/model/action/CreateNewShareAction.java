package de.ilume.nextcloud.outbound.model.action;

import de.ilume.nextcloud.outbound.model.Permissions;
import de.ilume.nextcloud.outbound.model.ShareType;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@TemplateSubType(id = "createNewShare", label = "Create New Share")
public record CreateNewShareAction(
    @TemplateProperty(
            label = "Path",
            id = "createNewSharePath",
            binding = @TemplateProperty.PropertyBinding(name = "action.path"),
            group = "createNewShare")
        @NotBlank
        String path,
    @TemplateProperty(
            label = "Share Type",
            id = "createNewShareShareType",
            binding = @TemplateProperty.PropertyBinding(name = "action.shareType"),
            type = TemplateProperty.PropertyType.Dropdown,
            group = "createNewShare")
        @NotNull
        ShareType shareType,
    @TemplateProperty(
            label = "Share With",
            description = "User/Group ID, Email, CircleID or Conversation Name",
            id = "createNewShareShareWith",
            binding = @TemplateProperty.PropertyBinding(name = "action.shareWith"),
            condition =
                @TemplateProperty.PropertyCondition(
                    property = "createNewShareShareType",
                    oneOf = {
                      "USER",
                      "GROUP",
                      "EMAIL",
                      "FEDERATED_CLOUD_SHARE",
                      "TALK_CONVERSATION"
                    }),
            group = "createNewShare")
        String shareWith,
    @TemplateProperty(
            label = "Allow Public Upload",
            description =
                "Allow public upload to a public shared folder (legacy attribute. Dont use for File upload request)",
            type = TemplateProperty.PropertyType.Boolean,
            defaultValueType = TemplateProperty.DefaultValueType.Boolean,
            defaultValue = "false",
            id = "createNewSharePublicUpload",
            binding = @TemplateProperty.PropertyBinding(name = "action.publicUpload"),
            group = "createNewShare",
            condition =
                @TemplateProperty.PropertyCondition(
                    property = "createNewShareShareType",
                    equals = "PUBLIC_LINK"))
        boolean publicUpload,
    @TemplateProperty(
            label = "Password",
            description =
                "Password to protect public link Share. Must contain at least 10 characters, incl. 1 uppercase, 1 lowercase, and 1 number.",
            id = "createNewSharePassword",
            binding = @TemplateProperty.PropertyBinding(name = "action.sharePassword"),
            group = "createNewShare",
            condition =
                @TemplateProperty.PropertyCondition(
                    property = "createNewShareShareType",
                    equals = "PUBLIC_LINK"),
            constraints =
                @TemplateProperty.PropertyConstraints(
                    pattern =
                        @TemplateProperty.Pattern(
                            value =
                                "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d@$!%*?&\\-_#+.,;:()=\\[\\]{}]{10,}$",
                            message =
                                "Password must be at least 10 characters long and include: 1 uppercase, 1 lowercase, and 1 number.")))
        String sharePassword,
    @TemplateProperty(
            label = "Permissions",
            type = TemplateProperty.PropertyType.Dropdown,
            id = "createNewSharePermissions",
            group = "createNewShare",
            binding = @TemplateProperty.PropertyBinding(name = "action.permissions"))
        @NotNull
        Permissions permissions,
    @TemplateProperty(
            label = "Expiration Date",
            id = "createNewShareExpireDate",
            description = "Format: 'YYYY-MM-DD'",
            group = "createNewShare",
            binding = @TemplateProperty.PropertyBinding(name = "action.expireDate"),
            condition =
                @TemplateProperty.PropertyCondition(
                    property = "createNewShareShareType",
                    equals = "PUBLIC_LINK"))
        String expireDate)
    implements NextcloudAction {
        @AssertTrue(message = "'shareWith' is required for the selected share type.")
        public boolean isShareWithValid() {
                if (shareType != ShareType.PUBLIC_LINK) {
                        return shareWith != null && !shareWith.isBlank();
                }
                return true;
        }
}
