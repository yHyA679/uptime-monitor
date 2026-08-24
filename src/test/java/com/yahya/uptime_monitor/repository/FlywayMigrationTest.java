package com.yahya.uptime_monitor.repository;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlywayMigrationTest {

    @Test
    void createsTheSchemaAndEnforcesCoreMonitoringConstraints() throws Exception {
        String url = "jdbc:h2:mem:flyway_schema_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/migration")
                .load();

        assertEquals(1, flyway.migrate().migrationsExecuted);

        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    insert into websites (name, url)
                    values ('Example', 'https://example.com')
                    """);

            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    insert into websites (name, url, check_interval_seconds)
                    values ('Too frequent', 'https://fast.example.com', 5)
                    """));
        }
    }
}
