package de.ilume.nextcloud.outbound;

import de.ilume.nextcloud.outbound.core.NextcloudCredentialsProvider;
import de.ilume.nextcloud.outbound.model.NextcloudAuthentication;
import io.camunda.connector.api.error.ConnectorException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NextcloudHttpClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(NextcloudHttpClient.class);

  private final HttpClient httpClient;
  private final NextcloudAuthentication authentication;

  public NextcloudHttpClient(NextcloudAuthentication authentication) {
    this.authentication = authentication;
    this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
  }

  public String postNextcloud(String apiPath, Map<String, Object> fields) {
    String fullUrl =
        apiPath.startsWith("http") ? apiPath : authentication.normalizedBaseUrl() + apiPath;

    try {
      String formData =
          fields.entrySet().stream()
              .map(
                  e ->
                      URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)
                          + "="
                          + URLEncoder.encode(
                              e.getValue() != null ? e.getValue().toString() : "",
                              StandardCharsets.UTF_8))
              .collect(Collectors.joining("&"));

      String authHeader = NextcloudCredentialsProvider.getAuthorizationHeader(authentication);

      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(fullUrl))
              .header("Authorization", authHeader)
              .header("OCS-APIRequest", "true")
              .header("Accept", "application/json")
              .header("Content-Type", "application/x-www-form-urlencoded")
              .POST(HttpRequest.BodyPublishers.ofString(formData))
              .build();

      LOGGER.debug("Sending POST request to Nextcloud OCS API: '{}'", fullUrl);
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        String errorMsg = String.format("Nextcloud OCS API returned unexpected status code: %d. Response: %s",
                response.statusCode(), response.body());
        LOGGER.error(errorMsg);
        throw new ConnectorException("FAIL", errorMsg);
      }
      return response.body();
    } catch (ConnectorException ce) {
      throw ce;
    } catch (Exception e) {
      String errorMsg = String.format("Failed to execute HTTP POST request to Nextcloud API at '%s'. Error: %s",
              fullUrl, e.getMessage());
      LOGGER.error(errorMsg, e);
      throw new ConnectorException("FAIL", errorMsg, e);
    }
  }
}
