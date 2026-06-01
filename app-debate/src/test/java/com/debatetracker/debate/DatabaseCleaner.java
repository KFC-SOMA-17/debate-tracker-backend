package com.debatetracker.debate;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.support.TransactionTemplate;

public class DatabaseCleaner implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        ApplicationContext context = SpringExtension.getApplicationContext(extensionContext);
        cleanup(context);
    }

    private void cleanup(ApplicationContext context) {
        EntityManager em = context.getBean(EntityManager.class);
        TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class);

        transactionTemplate.execute(action -> {
            em.clear();
            truncateTables(em);
            return null;
        });
    }

    private void truncateTables(EntityManager em) {
        List<String> tableNames = findTableNames(em);
        if (tableNames.isEmpty()) {
            return;
        }

        em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();
        try {
            for (String tableName : tableNames) {
                em.createNativeQuery("TRUNCATE TABLE `%s`".formatted(tableName)).executeUpdate();
            }
        } finally {
            em.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> findTableNames(EntityManager em) {
        String tableNameSelectQuery = """
                SELECT TABLE_NAME
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = (SELECT DATABASE())
                  AND TABLE_TYPE = 'BASE TABLE'
                """;
        return em.createNativeQuery(tableNameSelectQuery).getResultList();
    }
}
