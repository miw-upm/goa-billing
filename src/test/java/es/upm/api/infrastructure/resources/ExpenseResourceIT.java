package es.upm.api.infrastructure.resources;

import es.upm.api.domain.exceptions.NotFoundException;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.services.ExpenseService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ExpenseResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService expenseService;

    @Test
    @WithMockUser(roles = "admin")
    void shouldCreateExpense() throws Exception {
        UUID expenseId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagementId": "%s",
                  "amount": 50,
                  "date": "2026-03-20",
                  "description": "Taxi"
                }
                """.formatted(engagementId);

        Expense response = Expense.builder()
                .id(expenseId)
                .engagementId(engagementId)
                .amount(BigDecimal.valueOf(50))
                .date(LocalDate.of(2026, 3, 20))
                .description("Taxi")
                .build();

        when(this.expenseService.create(any())).thenReturn(response);

        this.mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.toString()))
                .andExpect(jsonPath("$.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.amount").value(50))
                .andExpect(jsonPath("$.date").value("2026-03-20"))
                .andExpect(jsonPath("$.description").value("Taxi"));

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
                // amount = 0
                """
                {
                  \"engagementId\": \"%s\",
                  \"amount\": 0,
                  \"date\": \"2026-03-20\",
                  \"description\": \"Taxi\"
                }
                """.formatted(java.util.UUID.randomUUID()),
                // description blank
                """
                {
                  \"engagementId\": \"%s\",
                  \"amount\": 10,
                  \"date\": \"2026-03-20\",
                  \"description\": \"\"
                }
                """.formatted(java.util.UUID.randomUUID()),
                // engagementId null
                """
                {
                  \"amount\": 10,
                  \"date\": \"2026-03-20\",
                  \"description\": \"Taxi\"
                }
                """,
                // date null
                """
                {
                  \"engagementId\": \"%s\",
                  \"amount\": 10,
                  \"description\": \"Taxi\"
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
                .engagementId(engagementId)
                .amount(BigDecimal.valueOf(50))
                .date(LocalDate.of(2026, 3, 20))
                .description("Taxi")
                .build();

        when(this.expenseService.readById(expenseId)).thenReturn(response);

        this.mockMvc.perform(get("/expenses/{id}", expenseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.toString()))
                .andExpect(jsonPath("$.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.amount").value(50))
                .andExpect(jsonPath("$.date").value("2026-03-20"))
                .andExpect(jsonPath("$.description").value("Taxi"));

        verify(this.expenseService).readById(expenseId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldUpdateExpense() throws Exception {
        UUID expenseId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        Expense response = Expense.builder()
                .id(expenseId)
                .engagementId(engagementId)
                .amount(BigDecimal.valueOf(65))
                .date(LocalDate.of(2026, 3, 21))
                .description("Updated taxi")
                .build();

        String requestBody = """
                {
                  "engagementId": "%s",
                  "amount": 65,
                  "date": "2026-03-21",
                  "description": "Updated taxi"
                }
                """.formatted(engagementId);

        when(this.expenseService.update(eq(expenseId), any())).thenReturn(response);

        this.mockMvc.perform(put("/expenses/{id}", expenseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.toString()))
                .andExpect(jsonPath("$.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.amount").value(65))
                .andExpect(jsonPath("$.date").value("2026-03-21"))
                .andExpect(jsonPath("$.description").value("Updated taxi"));

        verify(this.expenseService).update(eq(expenseId), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenUpdateAmountIsZero() throws Exception {
        UUID expenseId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagementId": "aaaaaaaa-bbbb-cccc-dddd-eeeeffff2000",
                  "amount": 0,
                  "description": "Updated taxi"
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
                when(this.expenseService.readById(expenseId))
                .thenThrow(new NotFoundException("Expense id: " + expenseId));

        this.mockMvc.perform(get("/expenses/{id}", expenseId))
                .andExpect(status().isNotFound());

        verify(this.expenseService).readById(expenseId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindAllWithValues() throws Exception {
        UUID expenseId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        Expense response = Expense.builder()
                .id(expenseId)
                .engagementId(engagementId)
                .amount(BigDecimal.valueOf(10))
                .date(LocalDate.of(2026, 3, 20))
                .description("Taxi")
                .build();

        when(this.expenseService.findAll()).thenReturn(Stream.of(response));

        this.mockMvc.perform(get("/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].id").value(expenseId.toString()))
                .andExpect(jsonPath("$.[0].engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.[0].amount").value(10))
                .andExpect(jsonPath("$.[0].date").value("2026-03-20"))
                .andExpect(jsonPath("$.[0].description").value("Taxi"));

        verify(this.expenseService).findAll();
    }
}
