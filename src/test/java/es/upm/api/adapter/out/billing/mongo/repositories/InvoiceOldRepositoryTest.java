package es.upm.api.adapter.out.billing.mongo.repositories;

import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceRepository;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.InvoiceOld;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataMongoTest
@ActiveProfiles("test")
class InvoiceOldRepositoryTest {

    private static final Sort DATE = Sort.by(Sort.Direction.DESC, "date");

    @Autowired
    private InvoiceRepository invoiceRepository;

    private InvoiceOld invoiceOld;

    @BeforeEach
    void setUp() {
        this.invoiceRepository.deleteAll();
        UUID engagementId = UUID.randomUUID();
        this.invoiceOld = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of(Expense.builder()
                        .id(UUID.randomUUID())
                        .engagementId(engagementId)
                        .amount(BigDecimal.valueOf(25))
                        .date(LocalDate.of(2026, 3, 20))
                        .description("Taxi")
                        .build()))
                .incomes(List.of(Income.builder()
                        .id(UUID.randomUUID())
                        .engagementId(engagementId)
                        .userId(UUID.randomUUID())
                        .amount(BigDecimal.valueOf(250))
                        .date(LocalDate.of(2026, 3, 20))
                        .build()))
                .build();
    }

    @Test
    void shouldSaveInvoice() {
        InvoiceEntity savedInvoiceEntity = this.invoiceRepository.save(new InvoiceEntity(this.invoiceOld));

        assertNotNull(savedInvoiceEntity);
        assertNotNull(savedInvoiceEntity.getId());
        assertEquals(this.invoiceOld.getId(), savedInvoiceEntity.getId());
        assertEquals(this.invoiceOld.getEngagementId(), savedInvoiceEntity.getEngagementId());
        assertEquals(this.invoiceOld.getDate(), savedInvoiceEntity.getDate());
        assertEquals(this.invoiceOld.getExpenses(), savedInvoiceEntity.getExpenses());
        assertEquals(this.invoiceOld.getIncomes(), savedInvoiceEntity.getIncomes());
    }

    @Test
    void shouldFindInvoiceById() {
        InvoiceEntity savedInvoiceEntity = this.invoiceRepository.save(new InvoiceEntity(this.invoiceOld));

        Optional<InvoiceEntity> optionalInvoiceEntity = this.invoiceRepository.findById(savedInvoiceEntity.getId());

        assertTrue(optionalInvoiceEntity.isPresent());
        InvoiceEntity foundInvoiceEntity = optionalInvoiceEntity.get();
        assertEquals(savedInvoiceEntity.getId(), foundInvoiceEntity.getId());
        assertEquals(savedInvoiceEntity.getEngagementId(), foundInvoiceEntity.getEngagementId());
        assertEquals(savedInvoiceEntity.getDate(), foundInvoiceEntity.getDate());
        assertEquals(savedInvoiceEntity.getExpenses(), foundInvoiceEntity.getExpenses());
        assertEquals(savedInvoiceEntity.getIncomes(), foundInvoiceEntity.getIncomes());
    }

    @Test
    void shouldFindInvoicesByEngagementId() {
        InvoiceEntity savedInvoiceEntity = this.invoiceRepository.save(new InvoiceEntity(this.invoiceOld));

        List<InvoiceEntity> foundInvoices = this.invoiceRepository.findByEngagementId(this.invoiceOld.getEngagementId(), DATE);

        assertEquals(1, foundInvoices.size());
        assertEquals(savedInvoiceEntity.getId(), foundInvoices.get(0).getId());
    }

    @Test
    void shouldReturnEmptyWhenEngagementIdDoesNotMatchAnyInvoice() {
        this.invoiceRepository.save(new InvoiceEntity(this.invoiceOld));

        List<InvoiceEntity> foundInvoices = this.invoiceRepository.findByEngagementId(UUID.randomUUID(), DATE);

        assertTrue(foundInvoices.isEmpty());
    }

    @Test
    void shouldFindInvoicesByDate() {
        InvoiceEntity savedInvoiceEntity = this.invoiceRepository.save(new InvoiceEntity(this.invoiceOld));

        List<InvoiceEntity> foundInvoices = this.invoiceRepository.findByDate(this.invoiceOld.getDate(), DATE);

        assertEquals(1, foundInvoices.size());
        assertEquals(savedInvoiceEntity.getId(), foundInvoices.get(0).getId());
    }

    @Test
    void shouldFindInvoicesByEngagementIdAndDate() {
        InvoiceEntity savedInvoiceEntity = this.invoiceRepository.save(new InvoiceEntity(this.invoiceOld));

        List<InvoiceEntity> foundInvoices = this.invoiceRepository.findByEngagementIdAndDate(
                this.invoiceOld.getEngagementId(), this.invoiceOld.getDate(), DATE
        );

        assertEquals(1, foundInvoices.size());
        assertEquals(savedInvoiceEntity.getId(), foundInvoices.get(0).getId());
    }

    @Test
    void shouldFindInvoiceByExpenseId() {
        InvoiceEntity savedInvoiceEntity = this.invoiceRepository.save(new InvoiceEntity(this.invoiceOld));
        UUID expenseId = this.invoiceOld.getExpenses().get(0).getId();

        InvoiceEntity foundInvoiceEntity = this.invoiceRepository.findByExpensesId(expenseId);

        assertNotNull(foundInvoiceEntity);
        assertEquals(savedInvoiceEntity.getId(), foundInvoiceEntity.getId());
    }

    @Test
    void shouldReturnNullWhenExpenseIdIsNotAssigned() {
        this.invoiceRepository.save(new InvoiceEntity(this.invoiceOld));

        InvoiceEntity foundInvoiceEntity = this.invoiceRepository.findByExpensesId(UUID.randomUUID());

        assertNull(foundInvoiceEntity);
    }

    @Test
    void shouldFindInvoiceByIncomeId() {
        InvoiceEntity savedInvoiceEntity = this.invoiceRepository.save(new InvoiceEntity(this.invoiceOld));
        UUID incomeId = this.invoiceOld.getIncomes().get(0).getId();

        InvoiceEntity foundInvoiceEntity = this.invoiceRepository.findByIncomesId(incomeId);

        assertNotNull(foundInvoiceEntity);
        assertEquals(savedInvoiceEntity.getId(), foundInvoiceEntity.getId());
    }

    @Test
    void shouldReturnNullWhenIncomeIdIsNotAssigned() {
        this.invoiceRepository.save(new InvoiceEntity(this.invoiceOld));

        InvoiceEntity foundInvoiceEntity = this.invoiceRepository.findByIncomesId(UUID.randomUUID());

        assertNull(foundInvoiceEntity);
    }
}
