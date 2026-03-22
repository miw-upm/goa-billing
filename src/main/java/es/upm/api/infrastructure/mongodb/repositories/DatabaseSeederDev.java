package es.upm.api.infrastructure.mongodb.repositories;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@Profile({"dev", "test"})
public class DatabaseSeederDev {

    private final ExpenseSeeder expenseSeeder;

    public DatabaseSeederDev(ExpenseSeeder expenseSeeder) {
        this.expenseSeeder = expenseSeeder;
    }

    @PostConstruct
    public void deleteAllAndInitializeAndSeedDataBase() {
        this.deleteAllAndInitialize();
        this.seedDataBaseJava();
    }

    private void deleteAllAndInitialize() {
        this.expenseSeeder.deleteAll();
        log.warn("------- Delete All -----------");
    }

    private void seedDataBaseJava() {
        this.expenseSeeder.seedDatabase();
        log.warn("------- Initial Load from JAVA ---------------------------------------------------------------");
    }

}
