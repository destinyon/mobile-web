package com.server.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class DatabaseMigration implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseMigration.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public DatabaseMigration(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureUsersPhone();
        ensureUsersEmail();
        ensureEmailLoginCodes();
    }

    private void ensureUsersPhone() {
        if (!columnExists("users", "phone") || !columnQueryable("users", "phone")) {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN phone VARCHAR(40)");
        }
        if (!indexExists("users", "uk_users_phone")) {
            tryCreateUniqueIndex("users", "uk_users_phone", "phone");
        }
    }

    private void ensureUsersEmail() {
        if (!columnExists("users", "email") || !columnQueryable("users", "email")) {
            jdbcTemplate.execute("ALTER TABLE users ADD COLUMN email VARCHAR(120)");
        }
        if (!indexExists("users", "uk_users_email")) {
            tryCreateUniqueIndex("users", "uk_users_email", "email");
        }
    }

    private void ensureEmailLoginCodes() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS email_login_codes (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    email VARCHAR(120) NOT NULL,
                    code_hash VARCHAR(128) NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    last_sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    verified_at TIMESTAMP NULL,
                    UNIQUE KEY uk_email_login_email (email)
                )
                """);
        if (!columnExists("email_login_codes", "email") || !columnQueryable("email_login_codes", "email")) {
            jdbcTemplate.execute("ALTER TABLE email_login_codes ADD COLUMN email VARCHAR(120) NOT NULL");
        }
        if (!columnExists("email_login_codes", "code_hash") || !columnQueryable("email_login_codes", "code_hash")) {
            jdbcTemplate.execute("ALTER TABLE email_login_codes ADD COLUMN code_hash VARCHAR(128)");
        }
        if (!columnExists("email_login_codes", "expires_at") || !columnQueryable("email_login_codes", "expires_at")) {
            jdbcTemplate.execute("ALTER TABLE email_login_codes ADD COLUMN expires_at TIMESTAMP NOT NULL");
        }
        if (!columnExists("email_login_codes", "last_sent_at") || !columnQueryable("email_login_codes", "last_sent_at")) {
            jdbcTemplate.execute("ALTER TABLE email_login_codes ADD COLUMN last_sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        }
        if (!columnExists("email_login_codes", "verified_at") || !columnQueryable("email_login_codes", "verified_at")) {
            jdbcTemplate.execute("ALTER TABLE email_login_codes ADD COLUMN verified_at TIMESTAMP NULL");
        }
        if (!indexExists("email_login_codes", "uk_email_login_email")) {
            tryCreateUniqueIndex("email_login_codes", "uk_email_login_email", "email");
        }
    }

    private void tryCreateUniqueIndex(String tableName, String indexName, String columnName) {
        String[] statements = {
                "CREATE UNIQUE INDEX " + indexName + " ON " + tableName + " (" + columnName + ")",
                "ALTER TABLE " + tableName + " ADD CONSTRAINT " + indexName + " UNIQUE (" + columnName + ")",
                "ALTER TABLE " + tableName + " ADD UNIQUE INDEX " + indexName + " (" + columnName + ")"
        };
        DataAccessException last = null;
        for (String statement : statements) {
            try {
                jdbcTemplate.execute(statement);
                return;
            } catch (DataAccessException ex) {
                String message = ex.getMostSpecificCause().getMessage();
                if (message != null && message.toLowerCase().contains("already exists")) {
                    return;
                }
                last = ex;
            }
        }
        log.warn("Failed to create unique index {} on {}({}); startup continues. Cause: {}",
                indexName, tableName, columnName,
                last == null ? "unknown" : last.getMostSpecificCause().getMessage());
    }

    private boolean columnExists(String tableName, String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            return metadataExists(connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName))
                    || metadataExists(connection.getMetaData().getColumns(connection.getCatalog(), null, tableName.toUpperCase(), columnName.toUpperCase()))
                    || metadataExists(connection.getMetaData().getColumns(null, null, tableName, columnName))
                    || metadataExists(connection.getMetaData().getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase()));
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect database column metadata", ex);
        }
    }

    private boolean columnQueryable(String tableName, String columnName) {
        try {
            jdbcTemplate.queryForList("SELECT " + columnName + " FROM " + tableName + " LIMIT 1");
            return true;
        } catch (DataAccessException ex) {
            return false;
        }
    }

    private boolean indexExists(String tableName, String indexName) {
        try (Connection connection = dataSource.getConnection()) {
            return indexMetadataExists(connection, tableName, indexName)
                    || indexMetadataExists(connection, tableName.toUpperCase(), indexName.toUpperCase());
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect database index metadata", ex);
        }
    }

    private boolean indexMetadataExists(Connection connection, String tableName, String indexName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (rs.next()) {
                String current = rs.getString("INDEX_NAME");
                if (current != null && current.equalsIgnoreCase(indexName)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean metadataExists(ResultSet rs) throws SQLException {
        try (rs) {
            return rs.next();
        }
    }
}
