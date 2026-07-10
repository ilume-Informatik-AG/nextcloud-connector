package de.ilume.nextcloud.outbound.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.ilume.nextcloud.outbound.utils.PathUtil;
import io.camunda.connector.generator.java.annotation.FeelMode;
import io.camunda.connector.generator.java.annotation.TemplateDiscriminatorProperty;
import io.camunda.connector.generator.java.annotation.TemplateProperty;
import io.camunda.connector.generator.java.annotation.TemplateSubType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    defaultImpl = NextcloudAuthentication.NextcloudBasicAuthentication.class)
@JsonSubTypes({
  @JsonSubTypes.Type(value = NextcloudAuthentication.NextcloudBasicAuthentication.class, name = "basic"),
})
@TemplateDiscriminatorProperty(
    label = "Authentication",
    group = "authentication",
    name = "type",
    defaultValue = "basic",
    description = "Specify Nextcloud authentication strategy")
public sealed interface NextcloudAuthentication permits NextcloudAuthentication.NextcloudBasicAuthentication {

    String normalizedBaseUrl();
    String user();

    @TemplateSubType(id = "basic", label = "Basic")
    record NextcloudBasicAuthentication(
            @TemplateProperty(
                    group = "authentication",
                    label = "Server url",
                    tooltip = "The url of the server. E.g. 'http://localhost:9000'",
                    feel = FeelMode.optional)
            @Pattern(regexp = "^(=|(http://|https://|secrets|\\{\\{).*$)", message = "Must be a http(s) URL")
                @NotBlank
                String url,
            @TemplateProperty(
                    group = "authentication",
                    label = "Username",
                    tooltip = "The username for authentication",
                    feel = FeelMode.optional)
                @NotBlank
                String user,
            @TemplateProperty(
                    group = "authentication",
                    tooltip = "The app password for authentication")
                @NotBlank
                String password) implements NextcloudAuthentication {
        public String normalizedBaseUrl() {
            return PathUtil.normalizeBaseUrl(url);
        }

        public String user() { return user; }
    }
}
