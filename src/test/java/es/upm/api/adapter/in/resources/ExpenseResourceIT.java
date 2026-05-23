package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.TaxCategory;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.services.ExpenseService;
import es.upm.miw.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ExpenseResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService expenseService;

    private final ExpenseFindCriteria criteria = new ExpenseFindCriteria();

    @Test
    @WithMockUser(roles = "admin")
    void shouldCreateExpense() throws Exception {
        UUID expenseId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagement": { "id": "%s" },
                  "baseAmount": 50,
                  "supplier": "Taxi Madrid",
                  "supplierIdentity": "A10000000",
                  "taxCategory": "OTROS"
                }
                """.formatted(engagementId);

        Expense response = Expense.builder()
                .id(expenseId)
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .baseAmount(BigDecimal.valueOf(50))
                .vatRate(BigDecimal.valueOf(21))
                .supplier("Taxi Madrid")
                .supplierIdentity("A10000000")
                .taxCategory(TaxCategory.OTROS)
                .issueDate(LocalDate.of(2026, 3, 20))
                .build();

        when(this.expenseService.create(any())).thenReturn(response);

        this.mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.toString()))
                .andExpect(jsonPath("$.engagement.id").value(engagementId.toString()))
                .andExpect(jsonPath("$.baseAmount").value(50))
                .andExpect(jsonPath("$.vatRate").value(21))
                .andExpect(jsonPath("$.supplier").value("Taxi Madrid"))
                .andExpect(jsonPath("$.supplierIdentity").value("A10000000"))
                .andExpect(jsonPath("$.taxCategory").value("OTROS"))
                .andExpect(jsonPath("$.date").value("2026-03-20"));

        verify(this.expenseService).create(any());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("badRequestExpenseBodies")
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenInvalidExpenseBody(String requestBody) throws Exception {
        this.mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
        verify(this.expenseService, never()).create(any());
    }

    static java.util.stream.Stream<String> badRequestExpenseBodies() {
        return java.util.stream.Stream.of(
                // baseAmount = 0
                """
                {
                  \"engagement\": { \"engagementId\": \"%s\" },
                  \"baseAmount\": 0,
                  \"supplier\": \"Taxi\",
                  \"supplierIdentity\": \"A10000000\",
                  \"taxCategory\": \"OTROS\"
                }
                """.formatted(java.util.UUID.randomUUID()),
                // supplier blank
                """
                {
                  \"engagement\": { \"engagementId\": \"%s\" },
                  \"baseAmount\": 10,
                  \"supplier\": \"\",
                  \"supplierIdentity\": \"A10000000\",
                  \"taxCategory\": \"OTROS\"
                }
                """.formatted(java.util.UUID.randomUUID()),
                // engagement null
                """
                {
                  \"baseAmount\": 10,
                  \"supplier\": \"Taxi\",
                  \"supplierIdentity\": \"A10000000\",
                  \"taxCategory\": \"OTROS\"
                }
                """,
                // taxCategory null
                """
                {
                  \"engagement\": { \"engagementId\": \"%s\" },
                  \"baseAmount\": 10,
                  \"supplier\": \"Taxi\",
                  \"supplierIdentity\": \"A10000000\"
                }
                """.formatted(java.util.UUID.randomUUID())
        );
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReadExpenseById() throws Exception {
        UUID expenseId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        Expense response = Expense.builder()
                .id(expenseId)
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .baseAmount(BigDecimal.valueOf(50))
                .vatRate(BigDecimal.valueOf(21))
                .supplier("Taxi Madrid")
                .supplierIdentity("A10000000")
                .taxCategory(TaxCategory.OTROS)
                .issueDate(LocalDate.of(2026, 3, 20))
                .build();

        when(this.expenseService.read(expenseId)).thenReturn(response);

        this.mockMvc.perform(get("/expenses/{id}", expenseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.toString()))
                .andExpect(jsonPath("$.engagement.id").value(engagementId.toString()))
                .andExpect(jsonPath("$.baseAmount").value(50))
                .andExpect(jsonPath("$.vatRate").value(21))
                .andExpect(jsonPath("$.supplier").value("Taxi Madrid"))
                .andExpect(jsonPath("$.supplierIdentity").value("A10000000"))
                .andExpect(jsonPath("$.taxCategory").value("OTROS"))
                .andExpect(jsonPath("$.date").value("2026-03-20"));

        verify(this.expenseService).read(expenseId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldUpdateExpense() throws Exception {
        UUID expenseId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        Expense response = Expense.builder()
                .id(expenseId)
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .baseAmount(BigDecimal.valueOf(65))
                .vatRate(BigDecimal.valueOf(21))
                .supplier("Taxi Updated")
                .supplierIdentity("A10000000")
                .taxCategory(TaxCategory.OTROS)
                .issueDate(LocalDate.of(2026, 3, 21))
                .build();

        String requestBody = """
                {
                  "engagement": { "id": "%s" },
                  "baseAmount": 65,
                  "supplier": "Taxi Updated",
                  "supplierIdentity": "A10000000",
                  "taxCategory": "OTROS"
                }
                """.formatted(engagementId);

        when(this.expenseService.update(eq(expenseId), any())).thenReturn(response);

        this.mockMvc.perform(put("/expenses/{id}", expenseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.toString()))
                .andExpect(jsonPath("$.engagement.id").value(engagementId.toString()))
                .andExpect(jsonPath("$.baseAmount").value(65))
                .andExpect(jsonPath("$.vatRate").value(21))
                .andExpect(jsonPath("$.supplier").value("Taxi Updated"))
                .andExpect(jsonPath("$.supplierIdentity").value("A10000000"))
                .andExpect(jsonPath("$.taxCategory").value("OTROS"))
                .andExpect(jsonPath("$.date").value("2026-03-21"));

        verify(this.expenseService).update(eq(expenseId), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenUpdateBaseAmountIsZero() throws Exception {
        UUID expenseId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagement": { "id": "aaaaaaaa-bbbb-cccc-dddd-eeeeffff2000" },
                  "baseAmount": 0,
                  "supplier": "Taxi Updated",
                  "supplierIdentity": "A10000000",
                  "taxCategory": "OTROS"
                }
                """;

        this.mockMvc.perform(put("/expenses/{id}", expenseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.expenseService, never()).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnNotFoundWhenExpenseDoesNotExist() throws Exception {
        UUID expenseId = UUID.randomUUID();
                when(this.expenseService.read(expenseId))
                .thenThrow(new NotFoundException("Expense id: " + expenseId));

        this.mockMvc.perform(get("/expenses/{id}", expenseId))
                .andExpect(status().isNotFound());

        verify(this.expenseService).read(expenseId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindWithValues() throws Exception {
        UUID expenseId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        Expense response = Expense.builder()
                .id(expenseId)
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .baseAmount(BigDecimal.valueOf(10))
                .vatRate(BigDecimal.valueOf(21))
                .supplier("Taxi Madrid")
                .supplierIdentity("A10000000")
                .taxCategory(TaxCategory.OTROS)
                .issueDate(LocalDate.of(2026, 3, 20))
                .build();

        when(this.expenseService.find(this.criteria)).thenReturn(Stream.of(response));

        this.mockMvc.perform(get("/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(expenseId.toString()))
                .andExpect(jsonPath("$.[0].engagement.id").value(engagementId.toString()))
                .andExpect(jsonPath("$.[0].baseAmount").value(10))
                .andExpect(jsonPath("$.[0].vatRate").value(21))
                .andExpect(jsonPath("$.[0].supplier").value("Taxi Madrid"))
                .andExpect(jsonPath("$.[0].supplierIdentity").value("A10000000"))
                .andExpect(jsonPath("$.[0].taxCategory").value("OTROS"))
                .andExpect(jsonPath("$.[0].date").value("2026-03-20"));

        verify(this.expenseService).find(this.criteria);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldDeleteExpense() throws Exception {
        UUID expenseId = UUID.randomUUID();

        this.mockMvc.perform(delete("/expenses/{id}", expenseId))
                .andExpect(status().isOk());

        verify(this.expenseService).delete(expenseId);
    }
}

