package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.SupplierInfo;
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
import java.util.List;
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
                  \"issueDate\": \"2026-03-20\",
                  \"baseAmount\": 0,
                  \"vatAmount\": 21,
                  \"supplier\": { \"name\": \"Taxi\", \"identity\": \"A10000000\" },
                  \"taxCategory\": \"OTROS\"
                }
                """.formatted(java.util.UUID.randomUUID()),
                // supplier blank
                """
                {
                  \"engagement\": { \"engagementId\": \"%s\" },
                  \"issueDate\": \"2026-03-20\",
                  \"baseAmount\": 10,
                  \"vatAmount\": 21,
                  \"supplier\": { \"name\": \"\", \"identity\": \"A10000000\" },
                  \"taxCategory\": \"OTROS\"
                }
                """.formatted(java.util.UUID.randomUUID()),
                // engagement null
                """
                {
                  \"baseAmount\": 10,
                  \"vatAmount\": 21,
                  \"supplier\": { \"name\": \"Taxi\", \"identity\": \"A10000000\" },
                  \"taxCategory\": \"OTROS\"
                }
                """,
                // taxCategory null
                """
                {
                  \"engagement\": { \"engagementId\": \"%s\" },
                  \"issueDate\": \"2026-03-20\",
                  \"baseAmount\": 10,
                  \"vatAmount\": 21,
                  \"supplier\": { \"name\": \"Taxi\", \"identity\": \"A10000000\" }
                }
                """.formatted(java.util.UUID.randomUUID())
        );
    }


    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenUpdateBaseAmountIsZero() throws Exception {
        UUID expenseId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagement": { "id": "aaaaaaaa-bbbb-cccc-dddd-eeeeffff2000" },
                  "issueDate": "2026-03-21",
                  "baseAmount": 0,
                  "vatAmount": 21,
                  "supplier": { "name": "Taxi Updated", "identity": "A10000000" },
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
    void shouldReturnExpenseCategories() throws Exception {
        this.mockMvc.perform(get("/expenses/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories.length()").value(TaxCategory.values().length))
                .andExpect(jsonPath("$.categories[0]").value("COMPRAS"))
                .andExpect(jsonPath("$.categories[" + (TaxCategory.values().length - 1) + "]").value("OTROS"));
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
