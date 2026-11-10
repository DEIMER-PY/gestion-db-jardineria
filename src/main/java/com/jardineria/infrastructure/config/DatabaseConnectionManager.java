package com.jardineria.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Gestor de conexiones a base de datos implementado bajo el Patrón Singleton con Double-Checked Locking.
 * Administra el pool de conexiones de alto rendimiento HikariCP para la base de datos de Jardinería.
 */
public final class DatabaseConnectionManager implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionManager.class);

    private static volatile DatabaseConnectionManager instance;
    private final HikariDataSource dataSource;

    /**
     * Constructor privado para prevenir instanciación directa (Patrón Singleton).
     */
    private DatabaseConnectionManager() {
        log.info("Inicializando DataSource Singleton con HikariCP para URL: {}", DatabaseConfig.getJdbcUrl());
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DatabaseConfig.getJdbcUrl());
        config.setUsername(DatabaseConfig.getUser());
        config.setPassword(DatabaseConfig.getPassword());
        config.setMaximumPoolSize(DatabaseConfig.getPoolSize());
        config.setMinimumIdle(DatabaseConfig.getMinIdle());
        config.setConnectionTimeout(DatabaseConfig.getConnectionTimeout());
        config.setPoolName("JardineriaHikariPool");

        // Optimizaciones recomendadas para MySQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        this.dataSource = new HikariDataSource(config);
        log.info("DataSource inicializado exitosamente.");

        // Hook de apagado ordenado para liberar conexiones y threads de MySQL
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "Shutdown-DatabasePool"));
    }

    /**
     * Obtiene la única instancia del gestor de conexiones (Thread-Safe con Double-Checked Locking).
     *
     * @return Instancia única de DatabaseConnectionManager
     */
    public static DatabaseConnectionManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnectionManager.class) {
                if (instance == null) {
                    instance = new DatabaseConnectionManager();
                }
            }
        }
        return instance;
    }

    /**
     * Obtiene una conexión activa del pool de conexiones.
     *
     * @return Conexión SQL
     * @throws SQLException si ocurre un error de conexión
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Retorna el DataSource subyacente.
     *
     * @return DataSource
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Cerrando DataSource de HikariCP...");
            dataSource.close();
            log.info("DataSource cerrado.");
        }
        try {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Exception ignored) {
        }
    }
}
