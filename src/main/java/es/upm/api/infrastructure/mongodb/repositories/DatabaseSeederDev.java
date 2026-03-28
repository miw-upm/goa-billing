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
    private final IncomeSeeder incomeSeeder;
    private final InvoiceSeeder invoiceSeeder;

    public DatabaseSeederDev(ExpenseSeeder expenseSeeder, IncomeSeeder incomeSeeder, InvoiceSeeder invoiceSeeder) {
        this.expenseSeeder = expenseSeeder;
        this.incomeSeeder = incomeSeeder;
        this.invoiceSeeder = invoiceSeeder;
    }

    @PostConstruct
    public void deleteAllAndInitializeAndSeedDataBase() {
        this.deleteAllAndInitialize();
        this.seedDataBaseJava();
    }

    private void deleteAllAndInitialize() {
        this.invoiceSeeder.deleteAll();
        this.expenseSeeder.deleteAll();
        this.incomeSeeder.deleteAll();
        log.warn("------- Delete All -----------");
    }

    private void seedDataBaseJava() {
        this.expenseSeeder.seedDatabase();
        this.incomeSeeder.seedDatabase();
        this.invoiceSeeder.seedDatabase();
        log.warn("------- Initial Load from JAVA ---------------------------------------------------------------");
    }

}
