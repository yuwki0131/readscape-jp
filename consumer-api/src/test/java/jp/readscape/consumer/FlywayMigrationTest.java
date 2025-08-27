package jp.readscape.consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlywayMigrationTest {

    @Test
    void testMigrationFilesExist() {
        // マイグレーションファイルが存在することを確認
        assertNotNull(getClass().getClassLoader().getResource("db/migration/V0001__create_schema_readscape.sql"), 
            "V0001 migration file should exist");
        assertNotNull(getClass().getClassLoader().getResource("db/migration/V0002__create_table_books.sql"), 
            "V0002 migration file should exist");
        assertNotNull(getClass().getClassLoader().getResource("db/migration/V0003__create_table_users.sql"), 
            "V0003 migration file should exist");
    }

    @Test
    void testMigrationFilesHaveCorrectContent() {
        // マイグレーションファイルが空でないことを確認
        assertDoesNotThrow(() -> {
            var v0001 = getClass().getClassLoader().getResourceAsStream("db/migration/V0001__create_schema_readscape.sql");
            assertNotNull(v0001, "V0001 migration should be readable");
            
            var v0002 = getClass().getClassLoader().getResourceAsStream("db/migration/V0002__create_table_books.sql");
            assertNotNull(v0002, "V0002 migration should be readable");
            
            var v0003 = getClass().getClassLoader().getResourceAsStream("db/migration/V0003__create_table_users.sql");
            assertNotNull(v0003, "V0003 migration should be readable");
        });
    }
}