package es.upm.api.adapter.out.billing.mongo.persistence;

import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceAdapter;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.InvoiceOld;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceEntity;
import es.upm.api.adapter.out.billing.mongo.invoice.InvoiceRepository;
import es.upm.miw.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class InvoiceOldAdapterIT {

    @Autowired
    private InvoiceAdapter invoicePersistenceMongodb;

    @MockitoBean
    private InvoiceRepository invoiceRepository;

    private InvoiceOld invoiceOld;

    @BeforeEach
    void setUp() {
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
    void shouldCreateInvoice() {
        when(this.invoiceRepository.save(any(InvoiceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        this.invoicePersistenceMongodb.create(this.invoiceOld);

        ArgumentCaptor<InvoiceEntity> invoiceEntityCaptor = ArgumentCaptor.forClass(InvoiceEntity.class);
        verify(this.invoiceRepository).save(invoiceEntityCaptor.capture());

        InvoiceEntity persistedInvoiceEntity = invoiceEntityCaptor.getValue();
        assertEquals(this.invoiceOld.getId(), persistedInvoiceEntity.getId());
        assertEquals(this.invoiceOld.getEngagementId(), persistedInvoiceEntity.getEngagementId());
        assertEquals(this.invoiceOld.getDate(), persistedInvoiceEntity.getDate());
        assertEquals(this.invoiceOld.getExpenses(), persistedInvoiceEntity.getExpenses());
        assertEquals(this.invoiceOld.getIncomes(), persistedInvoiceEntity.getIncomes());
    }

    @Test
    void shouldReadInvoiceById() {
        when(this.invoiceRepository.findById(this.invoiceOld.getId())).thenReturn(Optional.of(new InvoiceEntity(this.invoiceOld)));

        InvoiceOld readInvoiceOld = this.invoicePersistenceMongodb.readById(this.invoiceOld.getId());

        assertEquals(this.invoiceOld, readInvoiceOld);
        verify(this.invoiceRepository).findById(this.invoiceOld.getId());
    }

    @Test
    void shouldUpdateInvoice() {
        InvoiceOld updatedInvoiceOld = InvoiceOld.builder()
                .id(this.invoiceOld.getId())
                .engagementId(UUID.randomUUID())
                .date(LocalDate.of(2026, 3, 25))
                .expenses(List.of())
                .incomes(this.invoiceOld.getIncomes())
                .build();
        when(this.invoiceRepository.findById(this.invoiceOld.getId())).thenReturn(Optional.of(new InvoiceEntity(this.invoiceOld)));
        when(this.invoiceRepository.save(any(InvoiceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InvoiceOld persistedInvoiceOld = this.invoicePersistenceMongodb.update(this.invoiceOld.getId(), updatedInvoiceOld);

        assertEquals(updatedInvoiceOld, persistedInvoiceOld);
        verify(this.invoiceRepository).findById(this.invoiceOld.getId());
        verify(this.invoiceRepository).save(any(InvoiceEntity.class));
    }

    @Test
    void shouldFailReadInvoiceByIdWhenInvoiceDoesNotExist() {
        UUID invoiceId = UUID.randomUUID();
        when(this.invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> this.invoicePersistenceMongodb.readById(invoiceId));

        assertEquals("Not Found Exception. InvoiceOld id: " + invoiceId, thrown.getMessage());
        verify(this.invoiceRepository).findById(invoiceId);
    }

    @Test
    void shouldPropagateExceptionWhenRepositoryFails() {
        RuntimeException exception = new RuntimeException("Mongo error");
        when(this.invoiceRepository.save(any(InvoiceEntity.class))).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.invoicePersistenceMongodb.create(this.invoiceOld));

        assertEquals("Mongo error", thrown.getMessage());
        verify(this.invoiceRepository).save(any(InvoiceEntity.class));
    }

    @Test
    void shouldFailUpdateInvoiceWhenInvoiceDoesNotExist() {
        UUID invoiceId = UUID.randomUUID();
        when(this.invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
                () -> this.invoicePersistenceMongodb.update(invoiceId, this.invoiceOld));

        assertEquals("Not Found Exception. InvoiceOld id: " + invoiceId, thrown.getMessage());
        verify(this.invoiceRepository).findById(invoiceId);
    }

    @Test
    void shouldFindAllInvoices() {
        InvoiceFindCriteria criteria = new InvoiceFindCriteria();
        when(this.invoiceRepository.findAll(InvoiceAdapter.DATE)).thenReturn(List.of(new InvoiceEntity(this.invoiceOld)));

        List<InvoiceOld> invoiceOlds = this.invoicePersistenceMongodb.findAll(criteria).toList();

        assertEquals(1, invoiceOlds.size());
        assertEquals(this.invoiceOld, invoiceOlds.get(0));
        verify(this.invoiceRepository).findAll(InvoiceAdapter.DATE);
    }

    @Test
    void shouldFindInvoicesByEngagementId() {
        UUID engagementId = this.invoiceOld.getEngagementId();
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(engagementId, null);
        when(this.invoiceRepository.findByEngagementId(engagementId, InvoiceAdapter.DATE))
                .thenReturn(List.of(new InvoiceEntity(this.invoiceOld)));

        List<InvoiceOld> invoiceOlds = this.invoicePersistenceMongodb.findAll(criteria).toList();

        assertEquals(1, invoiceOlds.size());
        assertEquals(this.invoiceOld, invoiceOlds.get(0));
        verify(this.invoiceRepository).findByEngagementId(engagementId, InvoiceAdapter.DATE);
    }

    @Test
    void shouldReturnEmptyWhenNoInvoicesFoundByEngagementId() {
        UUID engagementId = UUID.randomUUID();
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(engagementId, null);
        when(this.invoiceRepository.findByEngagementId(engagementId, InvoiceAdapter.DATE)).thenReturn(List.of());

        List<InvoiceOld> invoiceOlds = this.invoicePersistenceMongodb.findAll(criteria).toList();

        assertEquals(0, invoiceOlds.size());
        verify(this.invoiceRepository).findByEngagementId(engagementId, InvoiceAdapter.DATE);
    }

    @Test
    void shouldFindInvoicesByDate() {
        LocalDate date = this.invoiceOld.getDate();
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(null, date);
        when(this.invoiceRepository.findByDate(date, InvoiceAdapter.DATE))
                .thenReturn(List.of(new InvoiceEntity(this.invoiceOld)));

        List<InvoiceOld> invoiceOlds = this.invoicePersistenceMongodb.findAll(criteria).toList();

        assertEquals(1, invoiceOlds.size());
        assertEquals(this.invoiceOld, invoiceOlds.get(0));
        verify(this.invoiceRepository).findByDate(date, InvoiceAdapter.DATE);
    }

    @Test
    void shouldFindInvoicesByEngagementIdAndDate() {
        UUID engagementId = this.invoiceOld.getEngagementId();
        LocalDate date = this.invoiceOld.getDate();
        InvoiceFindCriteria criteria = new InvoiceFindCriteria(engagementId, date);
        when(this.invoiceRepository.findByEngagementIdAndDate(engagementId, date, InvoiceAdapter.DATE))
                .thenReturn(List.of(new InvoiceEntity(this.invoiceOld)));

        List<InvoiceOld> invoiceOlds = this.invoicePersistenceMongodb.findAll(criteria).toList();

        assertEquals(1, invoiceOlds.size());
        assertEquals(this.invoiceOld, invoiceOlds.get(0));
        verify(this.invoiceRepository).findByEngagementIdAndDate(engagementId, date, InvoiceAdapter.DATE);
    }

    @Test
    void shouldFindInvoiceByExpenseId() {
        UUID expenseId = this.invoiceOld.getExpenses().get(0).getId();
        when(this.invoiceRepository.findByExpensesId(expenseId)).thenReturn(new InvoiceEntity(this.invoiceOld));

        InvoiceOld foundInvoiceOld = this.invoicePersistenceMongodb.findByExpenseId(expenseId);

        assertEquals(this.invoiceOld, foundInvoiceOld);
        verify(this.invoiceRepository).findByExpensesId(expenseId);
    }

    @Test
    void shouldReturnNullWhenExpenseIsNotAssigned() {
        UUID expenseId = this.invoiceOld.getExpenses().get(0).getId();
        when(this.invoiceRepository.findByExpensesId(expenseId)).thenReturn(null);

        InvoiceOld foundInvoiceOld = this.invoicePersistenceMongodb.findByExpenseId(expenseId);

        assertEquals(null, foundInvoiceOld);
        verify(this.invoiceRepository).findByExpensesId(expenseId);
    }

    @Test
    void shouldFindInvoiceByIncomeId() {
        UUID incomeId = this.invoiceOld.getIncomes().get(0).getId();
        when(this.invoiceRepository.findByIncomesId(incomeId)).thenReturn(new InvoiceEntity(this.invoiceOld));

        InvoiceOld foundInvoiceOld = this.invoicePersistenceMongodb.findByIncomeId(incomeId);

        assertEquals(this.invoiceOld, foundInvoiceOld);
        verify(this.invoiceRepository).findByIncomesId(incomeId);
    }

    @Test
    void shouldReturnNullWhenIncomeIsNotAssigned() {
        UUID incomeId = this.invoiceOld.getIncomes().get(0).getId();
        when(this.invoiceRepository.findByIncomesId(incomeId)).thenReturn(null);

        InvoiceOld foundInvoiceOld = this.invoicePersistenceMongodb.findByIncomeId(incomeId);

        assertEquals(null, foundInvoiceOld);
        verify(this.invoiceRepository).findByIncomesId(incomeId);
    }
}
