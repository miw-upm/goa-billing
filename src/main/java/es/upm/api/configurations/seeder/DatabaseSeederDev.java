package es.upm.api.configurations.seeder;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@Profile({"dev", "test"})
public class DatabaseSeederDev {

    private final ExpenseSeeder expenseSeeder;
    private final PaymentSeeder paymentSeeder;
    private final InvoiceSeeder invoiceSeeder;

    public DatabaseSeederDev(ExpenseSeeder expenseSeeder, PaymentSeeder paymentSeeder, InvoiceSeeder invoiceSeeder) {
        this.expenseSeeder = expenseSeeder;
        this.paymentSeeder = paymentSeeder;
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
        this.paymentSeeder.deleteAll();
        log.warn("------- Delete All -----------");
    }

    private void seedDataBaseJava() {
        this.expenseSeeder.seedDatabase();
        this.paymentSeeder.seedDatabase();
        this.invoiceSeeder.seedDatabase();
        log.warn("------- Initial Load from JAVA ---------------------------------------------------------------");
    }

}
