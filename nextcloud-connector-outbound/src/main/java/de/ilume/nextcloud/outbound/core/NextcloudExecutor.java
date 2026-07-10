package de.ilume.nextcloud.outbound.core;

import static de.ilume.nextcloud.outbound.utils.PathUtil.cleanResourcePath;
import static de.ilume.nextcloud.outbound.utils.PathUtil.normalizePath;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import de.ilume.nextcloud.outbound.NextcloudHttpClient;
import de.ilume.nextcloud.outbound.model.NextcloudAuthentication;
import de.ilume.nextcloud.outbound.model.NextcloudRequest;
import de.ilume.nextcloud.outbound.model.OcsResponseDTO;
import de.ilume.nextcloud.outbound.model.ShareType;
import de.ilume.nextcloud.outbound.model.action.*;
import de.ilume.nextcloud.outbound.model.response.*;
import de.ilume.nextcloud.outbound.utils.AppConfig;
import io.camunda.connector.api.document.Document;
import io.camunda.connector.api.document.DocumentCreationRequest;
import io.camunda.connector.api.error.ConnectorException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NextcloudExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(NextcloudExecutor.class);
  private static final long MAX_SIZE_FILE =
      Long.parseLong(AppConfig.getParameter("camunda.server.files.max-size", "10485760")); // 10mb
  // Buffers the upload stream into memory before sending it to Nextcloud. Needed because in some
  // Kubernetes environments the connection to Nextcloud can drop mid-stream, and a non-repeatable
  // InputStream can't be retried/replayed - buffering trades memory for upload reliability.
  private static final boolean BUFFER_UPLOAD_STREAM =
      Boolean.parseBoolean(AppConfig.getParameter("nextcloud.upload.buffer-stream", "true"));
  private static final String SLASH = "/";
  private static final String WEBDAV_PATH_PREFIX = "/remote.php/dav/files/";
  private static final String OCS_SHARE_PATH = "/ocs/v2.php/apps/files_sharing/api/v1/shares";
  private static final ObjectMapper MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private final Sardine sardine;
  private final NextcloudHttpClient nextcloudHttpClient;
  private final Function<DocumentCreationRequest, Document> createDocument;
  private final NextcloudAuthentication authentication;

  public NextcloudExecutor(
      Sardine sardine,
      NextcloudHttpClient nextcloudHttpClient,
      Function<DocumentCreationRequest, Document> createDocument,
      NextcloudAuthentication authentication) {
    this.sardine = sardine;
    this.nextcloudHttpClient = nextcloudHttpClient;
    this.createDocument = createDocument;
    this.authentication = authentication;
  }

  public static NextcloudExecutor create(
      NextcloudRequest nRequest, Function<DocumentCreationRequest, Document> createDocument) {

    NextcloudAuthentication auth = nRequest.authentication();

    Sardine realSardine = NextcloudCredentialsProvider.getSardine(auth);
    NextcloudHttpClient realHttpClient = NextcloudCredentialsProvider.getHttpClient(auth);

    return new NextcloudExecutor(realSardine, realHttpClient, createDocument, auth);
  }

  public Object execute(NextcloudAction action) {
    return switch (action) {
      case CopyFileAction copyFileAction -> copyFile(copyFileAction);
      case CreateFolderAction createFolderAction -> createFolder(createFolderAction);
      case CreateNewShareAction createNewShareAction -> createNewShare(createNewShareAction);
      case DeleteFileAction deleteFileAction -> deleteFile(deleteFileAction);
      case DownloadFileAction downloadFileAction -> downloadFile(downloadFileAction);
      case ListingFoldersAction listingFoldersAction -> listingFolders(listingFoldersAction);
      case MoveFileAction moveFileAction -> moveFile(moveFileAction);
      case UploadFileAction uploadFileAction -> uploadFile(uploadFileAction);
    };
  }

  private Object copyFile(CopyFileAction action) {
    String sourcePath = normalizePath(action.sourcePath() + SLASH + action.fileName());
    String targetPath = normalizePath(action.targetPath() + SLASH + action.fileName());

    String sourceUrl = buildFullWebDavUrl(sourcePath);
    String destinationUrl = buildFullWebDavUrl(targetPath);

    LOGGER.info("Copying file via WebDAV from '{}' to '{}'", sourceUrl, destinationUrl);

    try {
      this.sardine.copy(sourceUrl, destinationUrl);

      LOGGER.info("File successfully copied to '{}'", destinationUrl);

      return new NextcloudFileOperationResponse(
          NextcloudActionType.COPY_FILE, sourcePath, targetPath, action.fileName());

    } catch (Exception e) {
      LOGGER.error("Failed to copy file from '{}' to '{}'", sourceUrl, destinationUrl, e);

      String errorMessage =
          String.format(
              "Failed to copy file via Nextcloud WebDAV. Source: '%s', Destination: '%s'. Error: %s",
              sourceUrl, destinationUrl, e.getMessage());
      throw new ConnectorException("FAIL", errorMessage, e);
    }
  }

  private Object createFolder(CreateFolderAction action) {
    String targetPath = normalizePath(action.path() + SLASH + action.folderName());
    String url = buildFullWebDavUrl(targetPath);
    LOGGER.info("Creating directory via WebDAV at URL: '{}'", url);

    try {
      this.sardine.createDirectory(url);
      LOGGER.info("Directory '{}' successfully created", action.folderName());

      return new NextcloudFileOperationResponse(
          NextcloudActionType.CREATE_FOLDER, null, targetPath, action.folderName());
    } catch (IOException e) {
      LOGGER.error("Failed to create directory at '{}'", url, e);

      String errorMessage =
          String.format(
              "Failed to create directory via Nextcloud WebDAV. Path: '%s', Folder Name: '%s'. Error: %s",
              action.path(), action.folderName(), e.getMessage());

      throw new ConnectorException("FAIL", errorMessage, e);
    }
  }

  private Object createNewShare(CreateNewShareAction action) {
    String url = this.authentication.normalizedBaseUrl() + OCS_SHARE_PATH;

    Map<String, Object> params = new HashMap<>();
    params.put("path", action.path());
    params.put("shareType", action.shareType().getValue());

    if (action.permissions() != null) {
      params.put("permissions", action.permissions().getValue());
    }

    if (action.shareWith() != null) {
      params.put("shareWith", action.shareWith());
    }

    if (action.shareType() == ShareType.PUBLIC_LINK) {
      if (action.sharePassword() != null) {
        params.put("password", action.sharePassword());
      }
      if (action.expireDate() != null) {
        params.put("expireDate", action.expireDate());
      }
      if (action.publicUpload()) {
        params.put("publicUpload", true);
      }
    }

    LOGGER.info(
        "Sending Nextcloud OCS Share Request for path: '{}' (Type: {})",
        action.path(),
        action.shareType());
    LOGGER.debug("OCS Share Request Params: {}", params);

    try {
      String response = nextcloudHttpClient.postNextcloud(url, params);
      Map<String, Object> data = handleOcsResponse(response);

      String shareId = data.get("id") != null ? String.valueOf(data.get("id")) : null;
      String shareUrl = data.get("url") != null ? String.valueOf(data.get("url")) : null;
      String token = data.get("token") != null ? String.valueOf(data.get("token")) : null;
      String shareWith =
          data.get("share_with") != null ? String.valueOf(data.get("share_with")) : null;
      String permissions =
          data.get("permissions") != null ? String.valueOf(data.get("permissions")) : null;
      String expiration =
          data.get("expiration") != null ? String.valueOf(data.get("expiration")) : null;

      return new NextcloudShareResponse(
          NextcloudActionType.CREATE_NEW_SHARE,
          action.path(),
          shareId,
          shareUrl,
          token,
          shareWith,
          permissions,
          expiration);

    } catch (Exception e) {
      LOGGER.error("Failed to create Nextcloud share for path '{}'", action.path(), e);

      String errorMessage =
          String.format(
              "Failed to create Nextcloud share via OCS API. Path: '%s', ShareType: '%s'. Error: %s",
              action.path(), action.shareType(), e.getMessage());

      throw new ConnectorException("FAIL", errorMessage, e);
    }
  }

  private Object deleteFile(DeleteFileAction action) {
    String targetPath = normalizePath(action.path() + SLASH + action.fileName());
    String url = buildFullWebDavUrl(targetPath);
    LOGGER.info("Deleting file via WebDAV at URL: '{}'", url);

    try {
      this.sardine.delete(url);
      LOGGER.info("File successfully deleted from '{}'", url);

      return new NextcloudDeleteResponse(NextcloudActionType.DELETE_FILE, targetPath);

    } catch (IOException e) {
      LOGGER.error("Failed to delete file from '{}'", url, e);

      String errorMessage =
          String.format(
              "Failed to delete file via Nextcloud WebDAV. Path: '%s', File Name: '%s'. Error: %s",
              action.path(), action.fileName(), e.getMessage());

      throw new ConnectorException("FAIL", errorMessage, e);
    }
  }

  private Object downloadFile(DownloadFileAction action) {
    String targetPath = normalizePath(action.path() + SLASH + action.fileName());
    String url = buildFullWebDavUrl(targetPath);
    LOGGER.info("Downloading file via WebDAV at URL: '{}'", url);

    try {
      List<DavResource> resources = sardine.list(url, 0);
      if (resources.isEmpty()) {
        throw new ConnectorException(
            "FAIL",
            String.format(
                "File not found on Nextcloud: '%s/%s'", action.path(), action.fileName()));
      }

      DavResource remoteFile = resources.getFirst();

      if (remoteFile.getContentLength() > MAX_SIZE_FILE) {
        String sizeError =
            String.format(
                "File size (%d bytes) exceeds the allowed maximum limit of %d bytes.",
                remoteFile.getContentLength(), MAX_SIZE_FILE);
        LOGGER.error(sizeError);
        throw new ConnectorException("FAIL", sizeError);
      }

      try (InputStream nextcloudStream = this.sardine.get(url)) {
        String contentType =
            remoteFile.getContentType() != null
                ? remoteFile.getContentType()
                : "application/octet-stream";

        DocumentCreationRequest creationRequest =
            DocumentCreationRequest.from(nextcloudStream)
                .contentType(contentType)
                .fileName(action.fileName())
                .build();

        Document document = this.createDocument.apply(creationRequest);

        LOGGER.info(
            "File '{}' successfully downloaded and registered in Camunda Document Storage.",
            action.fileName());

        return new NextcloudDownloadResponse(
            NextcloudActionType.DOWNLOAD_FILE, targetPath, document);
      }
    } catch (IOException e) {
      LOGGER.error("WebDAV connection or streaming error during download of '{}'", url, e);
      throw new ConnectorException(
          "FAIL",
          String.format(
              "Failed to download file from Nextcloud or register it in Camunda. Path: '%s/%s'. Error: %s",
              action.path(), action.fileName(), e.getMessage()),
          e);
    }
  }

  private Object listingFolders(ListingFoldersAction action) {
    String url = buildFullWebDavUrl(action.path());
    LOGGER.info("Fetching Nextcloud directory resources from WebDAV URL: '{}'", url);

    try {
      List<DavResource> resources = List.of();

      switch (action.metadataSelection()) {
        case ALL -> {
          LOGGER.debug("Requesting ALL metadata attributes for depth: {}", action.depth());
          resources = this.sardine.list(url, action.depth(), true);
        }
        case CUSTOM -> {
          LOGGER.debug("Requesting CUSTOM metadata attributes: {}", action.additionalProperties());
          Set<QName> props = convertToQNameSet(action.additionalProperties());
          resources = this.sardine.list(url, action.depth(), props);
        }
        case DEFAULT -> {
          LOGGER.debug("Requesting DEFAULT metadata attributes for depth: {}", action.depth());
          resources = this.sardine.list(url, action.depth());
        }
      }

      LOGGER.info(
          "Successfully fetched {} resources from Nextcloud directory '{}'",
          resources.size(),
          action.path());

      String baseUrl = this.authentication.normalizedBaseUrl();
      String basePath = "";

      try {
        URI baseUri = URI.create(baseUrl);
        basePath = baseUri.getPath() != null ? baseUri.getPath() : "";
      } catch (Exception e) {
        // Fallback to empty path
      }

      String stripPrefix =
          normalizePath(basePath + SLASH + WEBDAV_PATH_PREFIX + SLASH + this.authentication.user());

      List<NextcloudResourceDto> dtos =
          resources.stream()
              .map(
                  res ->
                      new NextcloudResourceDto(
                          res.getName(),
                          cleanResourcePath(res.getPath(), stripPrefix),
                          res.isDirectory(),
                          res.getContentLength(),
                          res.getContentType(),
                          res.getEtag(),
                          res.getModified() != null ? res.getModified().toString() : null,
                          extractCustomProperties(res)))
              .toList();

      return new NextcloudListResponse(NextcloudActionType.LISTING_FOLDERS, action.path(), dtos);

    } catch (IOException e) {
      LOGGER.error("Failed to list directory contents at WebDAV URL: '{}'", url, e);

      String errorMessage =
          String.format(
              "Failed to list directory contents via Nextcloud WebDAV. Path: '%s', Depth: %d. Error: %s",
              action.path(), action.depth(), e.getMessage());

      throw new ConnectorException("FAIL", errorMessage, e);
    }
  }

  private Object moveFile(MoveFileAction action) {
    String sourcePath = normalizePath(action.sourcePath() + SLASH + action.fileName());
    String targetPath = normalizePath(action.targetPath() + SLASH + action.fileName());
    String sourceUrl = buildFullWebDavUrl(sourcePath);
    String destinationUrl = buildFullWebDavUrl(targetPath);

    LOGGER.info("Moving Nextcloud resource from '{}' to '{}'", sourceUrl, destinationUrl);

    try {
      this.sardine.move(sourceUrl, destinationUrl);

      LOGGER.info(
          "File '{}' successfully moved from '{}' to '{}'",
          action.fileName(),
          action.sourcePath(),
          action.targetPath());

      return new NextcloudFileOperationResponse(
          NextcloudActionType.MOVE_FILE, sourcePath, targetPath, action.fileName());

    } catch (IOException e) {
      LOGGER.error("WebDAV Move operation failed from '{}' to '{}'", sourceUrl, destinationUrl, e);

      String errorMessage =
          String.format(
              "Failed to move file via Nextcloud WebDAV. File: '%s', From: '%s', To: '%s'. Error: %s",
              action.fileName(), action.sourcePath(), action.targetPath(), e.getMessage());

      throw new ConnectorException("FAIL", errorMessage, e);
    }
  }

  private Object uploadFile(UploadFileAction action) {
    if (action.document() == null) {
      throw new ConnectorException(
          "FAIL", "Upload failed: No document was provided in the action payload.");
    }

    String fileName = action.document().metadata().getFileName();
    String contentType = action.document().metadata().getContentType();
    if (StringUtils.isBlank(contentType)) {
      contentType = "application/octet-stream";
    }

    String targetPath = normalizePath(action.path() + SLASH + fileName);
    String url = buildFullWebDavUrl(targetPath);
    LOGGER.info("Initiating file upload to Nextcloud WebDAV URL: '{}'", url);

    try (InputStream camundaStream = action.document().asInputStream()) {
      if (BUFFER_UPLOAD_STREAM) {
        LOGGER.debug(
            "Buffering Camunda document stream for file: '{}' ({})", fileName, contentType);
        // Buffer the file content from Camunda to prevent errors while uploading
        byte[] fileBytes = camundaStream.readAllBytes();
        this.sardine.put(url, fileBytes, contentType);
      } else {
        LOGGER.debug("Streaming Camunda document for file: '{}' ({})", fileName, contentType);
        this.sardine.put(
            url, camundaStream, contentType, true, action.document().metadata().getSize());
      }

      LOGGER.info(
          "File '{}' successfully uploaded to Nextcloud path '{}'", fileName, action.path());

      return new NextcloudFileOperationResponse(
          NextcloudActionType.UPLOAD_FILE, null, targetPath, fileName);

    } catch (Exception e) {
      LOGGER.error("Failed to upload file '{}' to Nextcloud URL '{}'", fileName, url, e);

      String errorMessage =
          String.format(
              "Failed to upload file to Nextcloud via WebDAV. Target Path: '%s', File Name: '%s'. Error: %s",
              action.path(), fileName, e.getMessage());

      throw new ConnectorException("FAIL", errorMessage, e);
    }
  }

  private String buildFullWebDavUrl(String path) {
    String baseUrl = this.authentication.normalizedBaseUrl();
    String user = this.authentication.user();

    String cleanPath = normalizePath(path);

    String combinedPath = normalizePath(WEBDAV_PATH_PREFIX + SLASH + user + cleanPath);

    try {
      URI baseUri = URI.create(baseUrl);
      String basePath = baseUri.getPath();

      String finalPath = combinedPath;
      if (basePath != null && !basePath.isEmpty() && !basePath.equals(SLASH)) {
        finalPath = normalizePath(basePath + combinedPath);
      }

      URI safeUri = new URI(baseUri.getScheme(), baseUri.getAuthority(), finalPath, null, null);

      return safeUri.toASCIIString();
    } catch (URISyntaxException e) {
      LOGGER.error("Failed to construct safe WebDAV URL for path '{}'", path, e);
      throw new ConnectorException("FAIL", "Invalid URL structure: " + e.getMessage(), e);
    }
  }

  private Map<String, Object> handleOcsResponse(String jsonResponse) {
    try {
      OcsResponseDTO dto = MAPPER.readValue(jsonResponse, OcsResponseDTO.class);

      if (dto.ocs() == null || dto.ocs().meta() == null) {
        throw new ConnectorException("FAIL", "Invalid OCS response structure from Nextcloud");
      }

      OcsResponseDTO.Meta meta = dto.ocs().meta();

      if (meta.statuscode() != 100 && meta.statuscode() != 200) {
        throw new ConnectorException(
            "FAIL",
            "Nextcloud OCS Error: " + meta.message() + " (Code: " + meta.statuscode() + ")");
      }

      return dto.ocs().data() != null ? dto.ocs().data() : Map.of();

    } catch (Exception e) {
      throw new ConnectorException(
          "FAIL", "Failed to parse Nextcloud OCS response: " + e.getMessage(), e);
    }
  }

  private Set<QName> convertToQNameSet(Map<String, String> map) {
    if (map == null || map.isEmpty()) {
      return Set.of();
    }

    return map.entrySet().stream()
        .map(entry -> new QName(entry.getKey().trim(), entry.getValue().trim()))
        .collect(Collectors.toSet());
  }

  private Map<String, String> extractCustomProperties(DavResource res) {
    if (res.getCustomProps() == null) {
      return Map.of();
    }
    return res.getCustomProps();
  }
}
