package de.ilume.nextcloud.outbound.utils;

/**
 * Reads deployment-tunable connector settings without depending on Spring, so lookups work
 * identically whether the connector runs standalone (Spring Boot RuntimeApplication) or is
 * mounted into an existing Connector Runtime's classpath, which may not run a Spring context for
 * this jar at all.
 */
public class AppConfig {

  private AppConfig() {}

  public static String getParameter(String key, String defaultValue) {
    String systemProperty = System.getProperty(key);
    if (systemProperty != null) {
      return systemProperty;
    }
    String envValue = System.getenv(toEnvVarName(key));
    return envValue != null ? envValue : defaultValue;
  }

  private static String toEnvVarName(String key) {
    return key.toUpperCase().replace('.', '_').replace('-', '_');
  }
}
