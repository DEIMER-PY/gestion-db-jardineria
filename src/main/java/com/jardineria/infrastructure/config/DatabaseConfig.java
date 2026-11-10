package com.jardineria.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gestor de configuración de base de datos que carga propiedades desde application.properties
 * permitiendo sobreescritura mediante variables de entorno del sistema.
 */
public final class DatabaseConfig {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final Properties props = new Properties();

    static {
        try (InputStream is = DatabaseConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
            } else {
                log.warn("No se encontró el archivo application.properties, usando valores predeterminados.");
            }
        } catch (IOException e) {
            log.error("Error al cargar application.properties", e);
        }
    }

    private DatabaseConfig() {
    }

    public static String getHost() {
        return getEnvOrProp("DB_HOST", "db.host", "localhost");
    }

    public static int getPort() {
        String portStr = getEnvOrProp("DB_PORT", "db.port", "3307");
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            return 3307;
        }
    }

    public static String getDatabaseName() {
        return getEnvOrProp("DB_NAME", "db.name", "jardineria");
    }

    public static String getUser() {
        return getEnvOrProp("DB_USER", "db.user", "root");
    }

    public static String getPassword() {
        return getEnvOrProp("DB_PASSWORD", "db.password", "password");
    }

    public static int getPoolSize() {
        String val = props.getProperty("db.pool.size", "10");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    public static int getMinIdle() {
        String val = props.getProperty("db.pool.minIdle", "2");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    public static long getConnectionTimeout() {
        String val = props.getProperty("db.pool.timeout", "30000");
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 30000L;
        }
    }

    public static String getJdbcUrl() {
        return String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8",
                getHost(), getPort(), getDatabaseName()
        );
    }

    private static String getEnvOrProp(String envKey, String propKey, String defaultValue) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        return props.getProperty(propKey, defaultValue);
    }
}
