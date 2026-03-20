package es.upm.api.infrastructure.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
}