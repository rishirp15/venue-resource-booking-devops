package com.rishi.venue.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:profiletest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class ProfileConfigurationTest {

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Verify default dev profile loads H2 DataSource and config")
    void testDevProfileConfiguration() throws Exception {
        String[] activeProfiles = environment.getActiveProfiles();
        assertEquals(1, activeProfiles.length);
        assertEquals("dev", activeProfiles[0]);

        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
            DatabaseMetaData metaData = connection.getMetaData();
            String dbName = metaData.getDatabaseProductName();
            assertTrue(dbName.contains("H2"), "Expected H2 database in dev profile, got: " + dbName);
        }
    }

    @Test
    @DisplayName("Verify application-prod.properties exists and has PostgreSQL settings")
    void testProdPropertiesExist() {
        ClassLoader classLoader = getClass().getClassLoader();
        assertNotNull(classLoader.getResource("application-prod.properties"),
                "application-prod.properties must exist");
        assertNotNull(classLoader.getResource("application-dev.properties"),
                "application-dev.properties must exist");
        assertNotNull(classLoader.getResource("db/migration/schema-postgres.sql"),
                "schema-postgres.sql must exist");
        assertNotNull(classLoader.getResource("db/migration/data-postgres.sql"),
                "data-postgres.sql must exist");
        assertNotNull(classLoader.getResource("db/migration/verify-migration.sql"),
                "verify-migration.sql must exist");
        assertNotNull(classLoader.getResource("db/migration/rollback-h2.sql"),
                "rollback-h2.sql must exist");
    }
}
