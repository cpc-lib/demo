package cc.ivera.ragdemo.service.ragops;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SqlCreateTableStatementExtractorTest {

    @Test
    void extractsOnlyCreateTableStatementsAndNormalizesBareCreateTable() {
        String sql = """
                -- baseline table
                CREATE TABLE `work_order`
                (
                    `id` BIGINT NOT NULL AUTO_INCREMENT,
                    PRIMARY KEY (`id`)
                ) ENGINE=InnoDB;

                INSERT INTO `work_order` (`id`) VALUES (1);

                CREATE TABLE IF NOT EXISTS `rag_knowledge_base`
                (
                    `id` BIGINT NOT NULL AUTO_INCREMENT,
                    PRIMARY KEY (`id`)
                ) ENGINE=InnoDB;

                DELIMITER //
                CREATE PROCEDURE `demo_proc`()
                BEGIN
                    SELECT 1;
                END//
                DELIMITER ;

                ALTER TABLE `rag_knowledge_base` ADD COLUMN `name` VARCHAR(128);
                CALL `demo_proc`();
                """;

        List<String> statements = SqlCreateTableStatementExtractor.extract(sql);

        assertThat(statements).hasSize(2);
        assertThat(statements.get(0)).startsWith("CREATE TABLE IF NOT EXISTS `work_order`");
        assertThat(statements.get(1)).startsWith("CREATE TABLE IF NOT EXISTS `rag_knowledge_base`");
        assertThat(statements)
                .noneMatch(statement -> statement.startsWith("INSERT"))
                .noneMatch(statement -> statement.startsWith("ALTER"))
                .noneMatch(statement -> statement.startsWith("CALL"))
                .noneMatch(statement -> statement.contains("CREATE PROCEDURE"));
    }
}
