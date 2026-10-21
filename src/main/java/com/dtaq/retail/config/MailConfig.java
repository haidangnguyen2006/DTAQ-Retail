package com.dtaq.retail.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static reader for SMTP configuration resolved from system properties, environment variables,
 * the {@code db.properties} file, then defaults.
 */
public final class MailConfig {

  private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

  private static final String DEFAULT_HOST = "smtp.gmail.com";
  private static final int DEFAULT_PORT = 587;
  private static final int DEFAULT_TIMEOUT_MS = 10_000;
  private static final String DEFAULT_STARTTLS = "true";

  private static Path propertiesPath = Paths.get("db.properties");
  private static volatile Properties fileProps;

  private MailConfig() {
  }

  /** Returns the SMTP host. */
  public static String host() {
    return resolve("DTAQ_SMTP_HOST", DEFAULT_HOST);
  }

  /** Returns the SMTP port, falling back to the default when the configured value is invalid. */
  public static int port() {
    return intValue("DTAQ_SMTP_PORT", DEFAULT_PORT);
  }

  /** Returns the SMTP account name. */
  public static String user() {
    return resolve("DTAQ_SMTP_USER", "");
  }

  /** Returns the SMTP account password. */
  public static String password() {
    return resolve("DTAQ_SMTP_PASSWORD", "");
  }

  /** Returns the sender address, falling back to the SMTP account name when unset. */
  public static String from() {
    String value = resolve("DTAQ_SMTP_FROM", "");
    return value.isBlank() ? user() : value.trim();
  }

  /** Returns whether STARTTLS is enabled, defaulting to {@code true}. */
  public static boolean startTls() {
    String value = resolve("DTAQ_SMTP_STARTTLS", DEFAULT_STARTTLS);
    String normalized = value == null ? "" : value.trim();
    if ("true".equalsIgnoreCase(normalized)) {
      return true;
    }
    if ("false".equalsIgnoreCase(normalized)) {
      return false;
    }
    log.warn("Invalid DTAQ_SMTP_STARTTLS value; using default {}", DEFAULT_STARTTLS);
    return true;
  }

  /** Returns the SMTP connection and read timeout in milliseconds. */
  public static int timeoutMs() {
    return intValue("DTAQ_SMTP_TIMEOUT_MS", DEFAULT_TIMEOUT_MS);
  }

  /** Returns true when both the SMTP account name and password are provided. */
  public static boolean isConfigured() {
    return !user().isBlank() && !password().isBlank();
  }

  private static int intValue(String key, int defaultValue) {
    String value = resolve(key, null);
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      log.warn("Invalid {} value; using default {}", key, defaultValue);
      return defaultValue;
    }
  }

  private static String resolve(String key, String defaultValue) {
    String value = System.getProperty(key);
    if (value == null) {
      value = System.getenv(key);
    }
    if (value == null) {
      value = fileProperties().getProperty(key);
    }
    return value == null ? defaultValue : value;
  }

  private static Properties fileProperties() {
    Properties current = fileProps;
    if (current == null) {
      synchronized (MailConfig.class) {
        current = fileProps;
        if (current == null) {
          current = load(propertiesPath);
          fileProps = current;
        }
      }
    }
    return current;
  }

  private static Properties load(Path path) {
    Properties properties = new Properties();
    if (!Files.isReadable(path)) {
      log.info("No mail configuration file found at {}", path.toAbsolutePath());
      return properties;
    }
    try (Reader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
      properties.load(reader);
    } catch (IOException e) {
      log.warn("Failed to load mail configuration from {}: {}", path.toAbsolutePath(), e.getMessage());
    }
    return properties;
  }

  static synchronized void setPropertiesFileForTest(Path path) {
    propertiesPath = (path == null ? Paths.get("db.properties") : path);
    fileProps = null;
  }
}
