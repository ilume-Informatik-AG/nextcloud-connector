package de.ilume.nextcloud.outbound.utils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class PathUtil {
    private static final String SLASH = "/";
    /**
     * Normalizes nextcloud paths.
     * - Returns "/" for empty or null paths.
     * - Guarantees leading slash.
     * - Removes duplicated slashes (e.g. "//" -> "/").
     * - Removes trailing slash (except for "/").
     */
    public static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return SLASH;
        }
        String normalized = path.trim().replaceAll("/{2,}", SLASH);
        if (!normalized.startsWith(SLASH)) {
            normalized = SLASH + normalized;
        }
        if (normalized.endsWith(SLASH) && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
    /**
     * Normalizes base urls (removes spaces and trailing slashes).
     */
    public static String normalizeBaseUrl(String url) {
        if (url == null) {
            return null;
        }
        String trimmed = url.trim();
        return trimmed.endsWith(SLASH) ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    /**
     * Cleans up a WebDAV resource path.
     * - Decodes url symbols (e.g. "%20" -> space).
     * - Removes the whole WebDAV prefix including user and context path.
     *
     * @param rawPath the raw path from sardine (e.g. "/nextcloud/remote.php/dav/files/user/My%20Folder")
     * @param prefixToStrip The prefix to remove (e.g. "/nextcloud/remote.php/dav/files/user")
     * @return The cleaned up relative path (e.g. "/My Folder")
     */
    public static String cleanResourcePath(String rawPath, String prefixToStrip) {
        if (rawPath == null) {
            return SLASH;
        }
        String decodedPath = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
        String normalizedPath = normalizePath(decodedPath);
        String normalizedPrefix = normalizePath(prefixToStrip);
        if (normalizedPath.startsWith(normalizedPrefix)) {
            String relative = normalizedPath.substring(normalizedPrefix.length());
            return normalizePath(relative);
        }
        return normalizedPath;
    }
}
