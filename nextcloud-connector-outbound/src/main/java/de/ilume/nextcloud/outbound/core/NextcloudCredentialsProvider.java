package de.ilume.nextcloud.outbound.core;

import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineImpl;
import de.ilume.nextcloud.outbound.NextcloudHttpClient;
import de.ilume.nextcloud.outbound.model.NextcloudAuthentication;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.http.HttpRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NextcloudCredentialsProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(NextcloudCredentialsProvider.class);

  public static NextcloudHttpClient getHttpClient(NextcloudAuthentication authentication) {
    if (authentication instanceof NextcloudAuthentication.NextcloudBasicAuthentication basicAuth) {
      return new NextcloudHttpClient(authentication);
    }
    throw new IllegalArgumentException(
        "Unsupported authentication type for HttpClient creation: "
            + authentication.getClass().getName());
  }

  /**
   * Initializes a Sardine client with preemptive authentication.
   *
   * <p><b>Note:</b> We use an {@link org.apache.http.HttpRequestInterceptor} to add the
   * Authorization header to every request immediately (preemptive authentication). This is crucial
   * for streaming file uploads using an {@link java.io.InputStream}.
   *
   * <p>Without this interceptor, the Apache HttpClient would perform a "reactive" authentication
   * (waiting for a 401 challenge), which triggers a request retry. Since InputStreams are
   * non-repeatable, a retry would result in a {@code
   * org.apache.http.client.NonRepeatableRequestException}.
   *
   * @param auth The Nextcloud authentication credentials.
   * @return A configured Sardine instance ready for streaming uploads.
   * @throws IllegalArgumentException if the authentication type is not supported.
   */
  public static Sardine getSardine(NextcloudAuthentication auth) {
    if (auth instanceof NextcloudAuthentication.NextcloudBasicAuthentication basicAuth) {

      HttpClientBuilder builder = HttpClients.custom();
      builder.addInterceptorFirst(
          (HttpRequest request, HttpContext context) ->
              request.addHeader("Authorization", getAuthorizationHeader(basicAuth)));
      Sardine sardine = new SardineImpl(builder);
      sardine.setCredentials(basicAuth.user(), basicAuth.password());

      return sardine;
    }
    throw new IllegalArgumentException(
        "Unsupported authentication type: " + auth.getClass().getName());
  }

  public static String getAuthorizationHeader(NextcloudAuthentication auth) {
    if (auth instanceof NextcloudAuthentication.NextcloudBasicAuthentication basicAuth) {
      String authString = basicAuth.user() + ":" + basicAuth.password();
      String base64Auth =
          Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));
      return "Basic " + base64Auth;
    }

    throw new IllegalArgumentException(
        "Unsupported authentication type: " + auth.getClass().getName());
  }
}
