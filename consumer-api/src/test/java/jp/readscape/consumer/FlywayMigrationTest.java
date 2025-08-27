package jp.readscape.consumer;

import org.junit.jupiter.api.Test;
import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class FlywayMigrationTest {

    @Test
    void testMigrationFilesExist() {
        // migrationファイルが統合ディレクトリに存在することを確認
        String migrationPath = "../infrastructure/database/migrations";
        File migrationDir = new File(migrationPath);
        
        assertTrue(migrationDir.exists(), "Migration directory should exist");
        assertTrue(new File(migrationDir, "V0001__create_schema_readscape.sql").exists(), "V0001 migration file should exist");
        assertTrue(new File(migrationDir, "V0002__create_table_books.sql").exists(), "V0002 migration file should exist");
        assertTrue(new File(migrationDir, "V0003__create_table_users.sql").exists(), "V0003 migration file should exist");
    }

    @Test
    void testMigrationFilesHaveCorrectContent() {
        // migrationファイルが空でないことを確認
        assertDoesNotThrow(() -> {
            String migrationPath = "../infrastructure/database/migrations";
            File migrationDir = new File(migrationPath);
            
            File v0001 = new File(migrationDir, "V0001__create_schema_readscape.sql");
            assertTrue(v0001.exists() && v0001.length() > 0, "V0001 migration should exist and not be empty");
            
            File v0002 = new File(migrationDir, "V0002__create_table_books.sql");
            assertTrue(v0002.exists() && v0002.length() > 0, "V0002 migration should exist and not be empty");
            
            File v0003 = new File(migrationDir, "V0003__create_table_users.sql");
            assertTrue(v0003.exists() && v0003.length() > 0, "V0003 migration should exist and not be empty");
        });
    }
}