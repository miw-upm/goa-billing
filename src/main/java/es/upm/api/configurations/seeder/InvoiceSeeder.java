package es.upm.api.configurations.seeder;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.infrastructure.mongodb.entities.InvoiceEntity;
import es.upm.api.infrastructure.mongodb.repositories.InvoiceRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Log4j2
@Repository
@Profile({"dev", "test"})
public class InvoiceSeeder {

    private final InvoiceRepository invoiceRepository;

    public InvoiceSeeder(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public void seedDatabase() {
        log.warn("------- Invoice Initial Load -----------");
        List<InvoiceEntity> invoices = List.of(
                this.buildInvoice(
                        "cccccccc-dddd-eeee-ffff-aaaabbbb0001",
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000",
                        LocalDate.of(2026, 3, 23),
                        List.of(
                                this.buildExpense(
                                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff1000",
                                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000",
                                        "35.50",
                                        LocalDate.of(2026, 3, 15),
                                        "Taxi from airport"
                                )
                        ),
                        List.of(
                                this.buildIncome(
                                        "bbbbbbbb-cccc-dddd-eeee-ffffaaaab001",
                                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0000",
                                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                                        "500.00",
                                        LocalDate.of(2026, 3, 20)
                                )
                        )
                ),
                this.buildInvoice(
                        "cccccccc-dddd-eeee-ffff-aaaabbbb0002",
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                        LocalDate.of(2026, 3, 24),
                        List.of(
                                this.buildExpense(
                                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff1001",
                                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                                        "120.00",
                                        LocalDate.of(2026, 3, 16),
                                        "Hotel accommodation"
                                )
                        ),
                        List.of(
                                this.buildIncome(
                                        "bbbbbbbb-cccc-dddd-eeee-ffffaaaab002",
                                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001",
                                        "1200.00",
                                        LocalDate.of(2026, 3, 21)
                                ),
                                this.buildIncome(
                                        "bbbbbbbb-cccc-dddd-eeee-ffffaaaab003",
                                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                                        "200.00",
                                        LocalDate.of(2026, 3, 22)
                                )
                        )
                ),
                this.buildInvoice(
                        "cccccccc-dddd-eeee-ffff-aaaabbbb0003",
                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                        LocalDate.of(2026, 3, 25),
                        List.of(
                                this.buildExpense(
                                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff1004",
                                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                                        "64.80",
                                        LocalDate.of(2026, 3, 18),
                                        "Client dinner"
                                )
                        ),
                        List.of(
                                this.buildIncome(
                                        "bbbbbbbb-cccc-dddd-eeee-ffffaaaab004",
                                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0001",
                                        "875.00",
                                        LocalDate.of(2026, 3, 23)
                                ),
                                this.buildIncome(
                                        "bbbbbbbb-cccc-dddd-eeee-ffffaaaab005",
                                        "aaaaaaa0-bbbb-cccc-dddd-eeeeffff0001",
                                        "aaaaaaaa-bbbb-cccc-dddd-eeeeffff0000",
                                        "325.00",
                                        LocalDate.of(2026, 3, 24)
                                )
                        )
                )

        );
        this.invoiceRepository.saveAll(invoices);
    }

    public void deleteAll() {
        this.invoiceRepository.deleteAll();
    }

    private InvoiceEntity buildInvoice(String id, String engagementId, LocalDate date, List<Expense> expenses, List<Income> incomes) {
        InvoiceEntity invoiceEntity = new InvoiceEntity();
        invoiceEntity.setId(UUID.fromString(id));
        invoiceEntity.setEngagementId(UUID.fromString(engagementId));
        invoiceEntity.setDate(date);
        invoiceEntity.setExpenses(expenses);
        invoiceEntity.setIncomes(incomes);
        return invoiceEntity;
    }

    private Expense buildExpense(String id, String engagementId, String amount, LocalDate date, String description) {
        Expense expense = new Expense();
        expense.setId(UUID.fromString(id));
        expense.setEngagementId(UUID.fromString(engagementId));
        expense.setAmount(new BigDecimal(amount));
        expense.setDate(date);
        expense.setDescription(description);
        return expense;
    }

    private Income buildIncome(String id, String engagementId, String userId, String amount, LocalDate date) {
        Income income = new Income();
        income.setId(UUID.fromString(id));
        income.setEngagementId(UUID.fromString(engagementId));
        income.setUserId(UUID.fromString(userId));
        income.setAmount(new BigDecimal(amount));
        income.setDate(date);
        return income;
    }
}
