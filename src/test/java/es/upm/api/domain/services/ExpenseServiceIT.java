package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.TaxCategory;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.engagement.EngagementFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ExpenseServiceIT {

    @Autowired
    private ExpenseService expenseService;

    @MockitoBean
    private ExpenseGateway expenseGateway;

    @MockitoBean
    private EngagementFinder engagementFinder;

    private Expense expense;
    private final ExpenseFindCriteria criteria = new ExpenseFindCriteria();
    private UUID engagementId;
    private EngagementSnapshot engagement;

    @BeforeEach
    void setUp() {
        this.engagementId = UUID.randomUUID();
        this.engagement = EngagementSnapshot.builder().id(this.engagementId).build();
        this.expense = Expense.builder()
                .engagement(this.engagement)
                .baseAmount(BigDecimal.valueOf(25))
                .vatRate(BigDecimal.valueOf(21))
                .supplier("Taxi Madrid")
                .supplierIdentity("A10000000")
                .taxCategory(TaxCategory.OTROS)
                .date(LocalDate.of(2026, 3, 20))
                .documentPath("doc/path")
                .build();
    }

    @Test
    void shouldCreateExpense() {
        when(this.engagementFinder.read(this.engagementId)).thenReturn(this.engagement);

        Expense createdExpense = this.expenseService.create(this.expense);

        assertNotNull(createdExpense);
        assertNotNull(createdExpense.getId());
        assertEquals(this.engagementId, createdExpense.getEngagement().getId());
        assertEquals(this.expense.getBaseAmount(), createdExpense.getBaseAmount());
        assertEquals(this.expense.getDate(), createdExpense.getDate());
        assertEquals(this.expense.getSupplier(), createdExpense.getSupplier());

        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(this.expenseGateway).create(expenseCaptor.capture());
        verify(this.engagementFinder).read(this.engagementId);

        Expense persistedExpense = expenseCaptor.getValue();
        assertNotNull(persistedExpense.getId());
        assertEquals(createdExpense.getId(), persistedExpense.getId());
        assertEquals(this.engagementId, persistedExpense.getEngagement().getId());
        assertEquals(this.expense.getBaseAmount(), persistedExpense.getBaseAmount());
        assertEquals(this.expense.getDate(), persistedExpense.getDate());
        assertEquals(this.expense.getSupplier(), persistedExpense.getSupplier());
    }

    @Test
    void shouldNotPersistExpenseWhenEngagementDoesNotExist() {
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.engagementFinder.read(this.engagementId)).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.expenseService.create(this.expense));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.engagementFinder).read(this.engagementId);
        verify(this.expenseGateway, never()).create(any());
    }

    @Test
    void shouldReadExpenseById() {
        this.expense.setId(UUID.randomUUID());
        when(this.expenseGateway.read(this.expense.getId())).thenReturn(this.expense);
        when(this.engagementFinder.read(this.engagementId)).thenReturn(this.engagement);

        Expense readExpense = this.expenseService.read(this.expense.getId());

        assertEquals(this.expense, readExpense);
        verify(this.expenseGateway).read(this.expense.getId());
        verify(this.engagementFinder).read(this.engagementId);
    }

    @Test
    void shouldFind() {
        Stream<Expense> expenseStream = Stream.of(this.expense);
        when(this.expenseGateway.find(this.criteria)).thenReturn(expenseStream);

        Stream<Expense> allExpenses = this.expenseService.find(this.criteria);

        verify(this.expenseGateway).find(this.criteria);
        assertEquals(this.expense, allExpenses.findFirst().orElse(null));
    }

    @Test
    void shouldUpdateExpense() {
        UUID id = UUID.randomUUID();
        Expense existing = Expense.builder()
                .id(id)
                .engagement(this.engagement)
                .baseAmount(BigDecimal.valueOf(10))
                .vatRate(BigDecimal.valueOf(21))
                .supplier("Old")
                .supplierIdentity("OLD")
                .taxCategory(TaxCategory.OTROS)
                .date(LocalDate.of(2026, 3, 20))
                .documentPath("old/doc")
                .build();

        Expense updateData = Expense.builder()
                .engagement(this.engagement)
                .baseAmount(BigDecimal.valueOf(90))
                .vatRate(BigDecimal.valueOf(21))
                .supplier("Updated supplier")
                .supplierIdentity("NEW")
                .taxCategory(TaxCategory.SUMINISTROS)
                .documentPath("ignored")
                .build();

        Expense updatedExpense = Expense.builder()
                .id(id)
                .engagement(this.engagement)
                .baseAmount(updateData.getBaseAmount())
                .vatRate(updateData.getVatRate())
                .supplier(updateData.getSupplier())
                .supplierIdentity(updateData.getSupplierIdentity())
                .taxCategory(updateData.getTaxCategory())
                .date(existing.getDate())
                .documentPath(existing.getDocumentPath())
                .build();

        when(this.expenseGateway.read(id)).thenReturn(existing);
        when(this.engagementFinder.read(this.engagementId)).thenReturn(this.engagement);
        when(this.expenseGateway.update(eq(id), any(Expense.class))).thenReturn(updatedExpense);

        Expense response = this.expenseService.update(id, updateData);

        assertEquals(updatedExpense, response);
        verify(this.expenseGateway).read(id);
        verify(this.engagementFinder).read(this.engagementId);
        verify(this.expenseGateway).update(eq(id), any(Expense.class));
    }

    @Test
    void shouldNotUpdateExpenseWhenEngagementDoesNotExist() {
        UUID id = UUID.randomUUID();
        Expense existing = Expense.builder()
                .id(id)
                .engagement(this.engagement)
                .baseAmount(BigDecimal.TEN)
                .vatRate(BigDecimal.valueOf(21))
                .supplier("Old")
                .supplierIdentity("OLD")
                .taxCategory(TaxCategory.OTROS)
                .date(LocalDate.of(2026, 3, 20))
                .documentPath("old/doc")
                .build();
        Expense updateData = Expense.builder()
                .engagement(this.engagement)
                .baseAmount(BigDecimal.valueOf(90))
                .vatRate(BigDecimal.valueOf(21))
                .supplier("Updated supplier")
                .supplierIdentity("NEW")
                .taxCategory(TaxCategory.SUMINISTROS)
                .build();
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.expenseGateway.read(id)).thenReturn(existing);
        when(this.engagementFinder.read(this.engagementId)).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.expenseService.update(id, updateData));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.expenseGateway).read(id);
        verify(this.engagementFinder).read(this.engagementId);
        verify(this.expenseGateway, never()).update(any(), any());
    }

    @Test
    void shouldDeleteExpense() {
        UUID id = UUID.randomUUID();

        this.expenseService.delete(id);

        verify(this.expenseGateway).delete(id);
    }
}
