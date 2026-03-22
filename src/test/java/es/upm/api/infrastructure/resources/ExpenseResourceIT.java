package es.upm.api.infrastructure.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ExpenseResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExpenseService expenseService;

    @Test
    @WithMockUser(roles = "admin")
    void shouldCreateExpense() throws Exception {
        UUID expenseId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();

        Expense request = Expense.builder()
                .engagementId(engagementId)
                .amount(BigDecimal.valueOf(50))
                .date(LocalDate.of(2026, 3, 20))
                .description("Taxi")
                .build();

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
                        .content(this.objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.toString()))
                .andExpect(jsonPath("$.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.amount").value(50))
                .andExpect(jsonPath("$.date").value("2026-03-20"))
                .andExpect(jsonPath("$.description").value("Taxi"));

        verify(this.expenseService).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenAmountIsZero() throws Exception {
        Expense request = Expense.builder()
                .engagementId(UUID.randomUUID())
                .amount(BigDecimal.ZERO)
                .date(LocalDate.of(2026, 3, 20))
                .description("Taxi")
                .build();

        this.mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(this.expenseService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenDescriptionIsBlank() throws Exception {
        Expense request = Expense.builder()
                .engagementId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(10))
                .date(LocalDate.of(2026, 3, 20))
                .description("")
                .build();

        this.mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(this.expenseService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenEngagementIdIsNull() throws Exception {
        Expense request = Expense.builder()
                .engagementId(null)
                .amount(BigDecimal.valueOf(10))
                .date(LocalDate.of(2026, 3, 20))
                .description("Taxi")
                .build();

        this.mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(this.expenseService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenDateIsNull() throws Exception {
        Expense request = Expense.builder()
                .engagementId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(10))
                .date(null)
                .description("Taxi")
                .build();

        this.mockMvc.perform(post("/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(this.expenseService, never()).create(any());
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
    void shouldReturnNotFoundWhenExpenseDoesNotExist() throws Exception {
        UUID expenseId = UUID.randomUUID();
        when(this.expenseService.readById(eq(expenseId)))
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
