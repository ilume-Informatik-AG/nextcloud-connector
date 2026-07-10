package de.ilume.nextcloud.outbound.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import de.ilume.nextcloud.outbound.NextcloudHttpClient;
import de.ilume.nextcloud.outbound.model.NextcloudAuthentication;
import de.ilume.nextcloud.outbound.model.NextcloudRequest;
import de.ilume.nextcloud.outbound.model.action.NextcloudActionType;
import de.ilume.nextcloud.outbound.model.action.UploadFileAction;
import de.ilume.nextcloud.outbound.model.response.NextcloudDeleteResponse;
import de.ilume.nextcloud.outbound.model.response.NextcloudDownloadResponse;
import de.ilume.nextcloud.outbound.model.response.NextcloudFileOperationResponse;
import de.ilume.nextcloud.outbound.model.response.NextcloudListResponse;
import de.ilume.nextcloud.outbound.model.response.NextcloudResourceDto;
import de.ilume.nextcloud.outbound.model.response.NextcloudShareResponse;
import io.camunda.connector.api.document.Document;
import io.camunda.connector.api.document.DocumentCreationRequest;
import io.camunda.connector.api.document.DocumentMetadata;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.runtime.test.outbound.OutboundConnectorContextBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.xml.namespace.QName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Loads the same JSON payloads a Camunda process would send (see {@code
 * src/test/resources/actions}), binds them into {@link NextcloudRequest} exactly like {@code
 * NextcloudConnectorFunction} does, and runs them through {@link NextcloudExecutor} against a
 * mocked {@link Sardine}/{@link NextcloudHttpClient} to verify the resulting WebDAV/OCS calls and
 * the mapped {@code NextcloudResponse}.
 */
class NextcloudExecutorTest {

  private static final String WEBDAV_PREFIX = "/remote.php/dav/files/testuser";

  private static final String OCS_SHARE_RESPONSE =
      """
      {
        "ocs": {
          "meta": {"status": "ok", "statuscode": 100, "message": "OK"},
          "data": {
            "id": 42,
            "url": "http://localhost:9000/s/abc123",
            "token": "abc123",
            "share_with": "jdoe",
            "permissions": 1,
            "expiration": "2026-12-31 00:00:00"
          }
        }
      }
      """;

  private Sardine sardine;
  private NextcloudHttpClient httpClient;

  @BeforeEach
  void setUp() {
    sardine = mock(Sardine.class);
    httpClient = mock(NextcloudHttpClient.class);
  }

  private static NextcloudRequest loadRequest(String fixture) {
    try (InputStream is =
        NextcloudExecutorTest.class.getResourceAsStream("/actions/" + fixture)) {
      if (is == null) {
        throw new IllegalStateException("Fixture not found on classpath: actions/" + fixture);
      }
      String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      OutboundConnectorContext context =
          OutboundConnectorContextBuilder.create().variables(json).build();
      return context.bindVariables(NextcloudRequest.class);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private NextcloudExecutor executorFor(NextcloudAuthentication authentication) {
    Function<DocumentCreationRequest, Document> createDocument = req -> mock(Document.class);
    return new NextcloudExecutor(sardine, httpClient, createDocument, authentication);
  }

  private static DavResource davResource(
      String name, String path, boolean directory, long contentLength, String contentType) {
    DavResource resource = mock(DavResource.class);
    when(resource.getName()).thenReturn(name);
    when(resource.getPath()).thenReturn(path);
    when(resource.isDirectory()).thenReturn(directory);
    when(resource.getContentLength()).thenReturn(contentLength);
    when(resource.getContentType()).thenReturn(contentType);
    when(resource.getEtag()).thenReturn("etag-" + name);
    when(resource.getModified()).thenReturn(null);
    when(resource.getCustomProps()).thenReturn(Map.of());
    return resource;
  }

  @Test
  void copiesFileViaWebDav() throws Exception {
    NextcloudRequest request = loadRequest("copy-file.json");
    NextcloudExecutor executor = executorFor(request.authentication());

    Object result = executor.execute(request.action());

    verify(sardine)
        .copy(
            eq("http://localhost:9000" + WEBDAV_PREFIX + "/documents/source/report.pdf"),
            eq("http://localhost:9000" + WEBDAV_PREFIX + "/documents/target/report.pdf"));

    assertThat(result)
        .isInstanceOfSatisfying(
            NextcloudFileOperationResponse.class,
            response -> {
              assertThat(response.actionType()).isEqualTo(NextcloudActionType.COPY_FILE);
              assertThat(response.source()).isEqualTo("/documents/source/report.pdf");
              assertThat(response.target()).isEqualTo("/documents/target/report.pdf");
              assertThat(response.fileName()).isEqualTo("report.pdf");
            });
  }

  @Test
  void createsFolderViaWebDav() throws Exception {
    NextcloudRequest request = loadRequest("create-folder.json");
    NextcloudExecutor executor = executorFor(request.authentication());

    Object result = executor.execute(request.action());

    verify(sardine).createDirectory("http://localhost:9000" + WEBDAV_PREFIX + "/documents/invoices");

    assertThat(result)
        .isInstanceOfSatisfying(
            NextcloudFileOperationResponse.class,
            response -> {
              assertThat(response.actionType()).isEqualTo(NextcloudActionType.CREATE_FOLDER);
              assertThat(response.source()).isNull();
              assertThat(response.target()).isEqualTo("/documents/invoices");
              assertThat(response.fileName()).isEqualTo("invoices");
            });
  }

  @Test
  @SuppressWarnings("unchecked")
  void createsUserShareViaOcsApi() {
    NextcloudRequest request = loadRequest("create-new-share-user.json");
    when(httpClient.postNextcloud(anyString(), anyMap())).thenReturn(OCS_SHARE_RESPONSE);
    NextcloudExecutor executor = executorFor(request.authentication());

    Object result = executor.execute(request.action());

    var paramsCaptor = ArgumentCaptor.forClass(Map.class);
    verify(httpClient)
        .postNextcloud(
            eq("http://localhost:9000/ocs/v2.php/apps/files_sharing/api/v1/shares"),
            paramsCaptor.capture());
    Map<String, Object> params = paramsCaptor.getValue();
    assertThat(params)
        .containsEntry("path", "/documents/report.pdf")
        .containsEntry("shareType", "0")
        .containsEntry("shareWith", "jdoe")
        .containsEntry("permissions", 1)
        .doesNotContainKey("password")
        .doesNotContainKey("expireDate");

    assertThat(result)
        .isInstanceOfSatisfying(
            NextcloudShareResponse.class,
            response -> {
              assertThat(response.actionType()).isEqualTo(NextcloudActionType.CREATE_NEW_SHARE);
              assertThat(response.target()).isEqualTo("/documents/report.pdf");
              assertThat(response.shareId()).isEqualTo("42");
              assertThat(response.url()).isEqualTo("http://localhost:9000/s/abc123");
              assertThat(response.token()).isEqualTo("abc123");
              assertThat(response.shareWith()).isEqualTo("jdoe");
            });
  }

  @Test
  @SuppressWarnings("unchecked")
  void createsPublicLinkShareViaOcsApi() {
    NextcloudRequest request = loadRequest("create-new-share-public-link.json");
    when(httpClient.postNextcloud(anyString(), anyMap())).thenReturn(OCS_SHARE_RESPONSE);
    NextcloudExecutor executor = executorFor(request.authentication());

    executor.execute(request.action());

    var paramsCaptor = ArgumentCaptor.forClass(Map.class);
    verify(httpClient).postNextcloud(anyString(), paramsCaptor.capture());
    Map<String, Object> params = paramsCaptor.getValue();
    assertThat(params)
        .containsEntry("path", "/documents/report.pdf")
        .containsEntry("shareType", "3")
        .containsEntry("password", "Sup3rSecret!")
        .containsEntry("expireDate", "2026-12-31")
        .containsEntry("publicUpload", true)
        .doesNotContainKey("shareWith");
  }

  @Test
  void deletesFileViaWebDav() throws Exception {
    NextcloudRequest request = loadRequest("delete-file.json");
    NextcloudExecutor executor = executorFor(request.authentication());

    Object result = executor.execute(request.action());

    verify(sardine).delete("http://localhost:9000" + WEBDAV_PREFIX + "/documents/old-report.pdf");

    assertThat(result)
        .isInstanceOfSatisfying(
            NextcloudDeleteResponse.class,
            response -> {
              assertThat(response.actionType()).isEqualTo(NextcloudActionType.DELETE_FILE);
              assertThat(response.target()).isEqualTo("/documents/old-report.pdf");
            });
  }

  @Test
  void downloadsFileAndRegistersDocument() throws Exception {
    NextcloudRequest request = loadRequest("download-file.json");
    String url = "http://localhost:9000" + WEBDAV_PREFIX + "/documents/report.pdf";

    DavResource remoteFile = davResource("report.pdf", url, false, 12L, "application/pdf");
    when(sardine.list(url, 0)).thenReturn(List.of(remoteFile));
    when(sardine.get(url))
        .thenReturn(new ByteArrayInputStream("hello world!".getBytes(StandardCharsets.UTF_8)));

    DocumentCreationRequest[] captured = new DocumentCreationRequest[1];
    Function<DocumentCreationRequest, Document> createDocument =
        req -> {
          captured[0] = req;
          return mock(Document.class);
        };
    NextcloudExecutor executor =
        new NextcloudExecutor(sardine, httpClient, createDocument, request.authentication());

    Object result = executor.execute(request.action());

    assertThat(captured[0].fileName()).isEqualTo("report.pdf");
    assertThat(captured[0].contentType()).isEqualTo("application/pdf");
    assertThat(captured[0].content().readAllBytes())
        .isEqualTo("hello world!".getBytes(StandardCharsets.UTF_8));

    assertThat(result)
        .isInstanceOfSatisfying(
            NextcloudDownloadResponse.class,
            response -> {
              assertThat(response.actionType()).isEqualTo(NextcloudActionType.DOWNLOAD_FILE);
              assertThat(response.target()).isEqualTo("/documents/report.pdf");
              assertThat(response.document()).isNotNull();
            });
  }

  @Test
  void failsDownloadWhenFileNotFound() throws Exception {
    NextcloudRequest request = loadRequest("download-file.json");
    String url = "http://localhost:9000" + WEBDAV_PREFIX + "/documents/report.pdf";
    when(sardine.list(eq(url), eq(0))).thenReturn(List.of());
    NextcloudExecutor executor = executorFor(request.authentication());

    assertThat(
            assertThrows(
                ConnectorException.class, () -> executor.execute(request.action())))
        .hasMessageContaining("File not found");
  }

  @Test
  void listsFolderWithDefaultMetadata() throws Exception {
    NextcloudRequest request = loadRequest("listing-folders-default.json");
    String url = "http://localhost:9000" + WEBDAV_PREFIX + "/documents";

    DavResource folder =
        davResource("documents", WEBDAV_PREFIX + "/documents/", true, 0L, null);
    DavResource file =
        davResource(
            "report.pdf", WEBDAV_PREFIX + "/documents/report.pdf", false, 2048L, "application/pdf");
    when(sardine.list(url, 1)).thenReturn(List.of(folder, file));

    Object result = executorFor(request.authentication()).execute(request.action());

    assertThat(result)
        .isInstanceOfSatisfying(
            NextcloudListResponse.class,
            response -> {
              assertThat(response.actionType()).isEqualTo(NextcloudActionType.LISTING_FOLDERS);
              assertThat(response.target()).isEqualTo("/documents");
              assertThat(response.resources()).hasSize(2);
              assertThat(response.resources())
                  .extracting(
                      NextcloudResourceDto::name,
                      NextcloudResourceDto::path,
                      NextcloudResourceDto::isDirectory)
                  .containsExactlyInAnyOrder(
                      tuple("documents", "/documents", true),
                      tuple("report.pdf", "/documents/report.pdf", false));
            });
  }

  @Test
  void listsFolderWithCustomMetadataProperties() throws Exception {
    NextcloudRequest request = loadRequest("listing-folders-custom.json");
    String url = "http://localhost:9000" + WEBDAV_PREFIX + "/documents";
    when(sardine.list(eq(url), eq(1), anySet())).thenReturn(List.of());

    executorFor(request.authentication()).execute(request.action());

    var propsCaptor = ArgumentCaptor.forClass(Set.class);
    verify(sardine).list(eq(url), eq(1), propsCaptor.capture());
    assertThat((Set<QName>) propsCaptor.getValue()).containsExactly(new QName("oc", "fileid"));
  }

  @Test
  void listsFolderWithAllMetadata() throws Exception {
    NextcloudRequest request = loadRequest("listing-folders-all.json");
    String url = "http://localhost:9000" + WEBDAV_PREFIX + "/documents";
    when(sardine.list(url, 1, true)).thenReturn(List.of());

    executorFor(request.authentication()).execute(request.action());

    verify(sardine).list(url, 1, true);
  }

  @Test
  void movesFileViaWebDav() throws Exception {
    NextcloudRequest request = loadRequest("move-file.json");
    NextcloudExecutor executor = executorFor(request.authentication());

    Object result = executor.execute(request.action());

    verify(sardine)
        .move(
            eq("http://localhost:9000" + WEBDAV_PREFIX + "/documents/source/report.pdf"),
            eq("http://localhost:9000" + WEBDAV_PREFIX + "/documents/target/report.pdf"));

    assertThat(result)
        .isInstanceOfSatisfying(
            NextcloudFileOperationResponse.class,
            response -> {
              assertThat(response.actionType()).isEqualTo(NextcloudActionType.MOVE_FILE);
              assertThat(response.source()).isEqualTo("/documents/source/report.pdf");
              assertThat(response.target()).isEqualTo("/documents/target/report.pdf");
            });
  }

  @Test
  void uploadsDocumentViaWebDav() throws Exception {
    NextcloudAuthentication authentication =
        new NextcloudAuthentication.NextcloudBasicAuthentication(
            "http://localhost:9000", "testuser", "testpassword");

    DocumentMetadata metadata = mock(DocumentMetadata.class);
    when(metadata.getFileName()).thenReturn("invoice.pdf");
    when(metadata.getContentType()).thenReturn("application/pdf");
    when(metadata.getSize()).thenReturn(11L);

    Document document = mock(Document.class);
    when(document.metadata()).thenReturn(metadata);
    when(document.asInputStream())
        .thenReturn(new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8)));

    UploadFileAction action = new UploadFileAction("/documents", document);
    NextcloudExecutor executor = executorFor(authentication);

    Object result = executor.execute(action);

    verify(sardine)
        .put(
            eq("http://localhost:9000" + WEBDAV_PREFIX + "/documents/invoice.pdf"),
            eq("hello world".getBytes(StandardCharsets.UTF_8)),
            eq("application/pdf"));

    assertThat(result)
        .isInstanceOfSatisfying(
            NextcloudFileOperationResponse.class,
            response -> {
              assertThat(response.actionType()).isEqualTo(NextcloudActionType.UPLOAD_FILE);
              assertThat(response.source()).isNull();
              assertThat(response.target()).isEqualTo("/documents/invoice.pdf");
              assertThat(response.fileName()).isEqualTo("invoice.pdf");
            });
  }

  @Test
  void uploadFailsWhenNoDocumentProvided() {
    NextcloudAuthentication authentication =
        new NextcloudAuthentication.NextcloudBasicAuthentication(
            "http://localhost:9000", "testuser", "testpassword");
    UploadFileAction action = new UploadFileAction("/documents", null);
    NextcloudExecutor executor = executorFor(authentication);

    assertThat(
            assertThrows(
                ConnectorException.class, () -> executor.execute(action)))
        .hasMessageContaining("No document was provided");
  }
}
