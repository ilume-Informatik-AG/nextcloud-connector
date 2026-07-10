package de.ilume.nextcloud.outbound.model.response;

import java.util.Map;

public record NextcloudResourceDto(
  String name,
  String path,
  boolean isDirectory,
  long size,
  String contentType,
  String etag,
  String modified,
  Map<String, String> customProperties
) {}
