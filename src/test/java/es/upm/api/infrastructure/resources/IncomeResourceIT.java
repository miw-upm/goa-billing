package es.upm.api.infrastructure.resources;

import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.services.IncomeService;
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
class IncomeResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncomeService incomeService;

    @Test
    @WithMockUser(roles = "admin")
    void shouldCreateIncome() throws Exception {
        UUID incomeId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String requestBody = """
                {
                  "engagementId": "%s",
                  "userId": "%s",
                  "amount": 350,
                  "date": "2026-03-20"
                }
                """.formatted(engagementId, userId);

        Income response = Income.builder()
                .id(incomeId)
                .engagementId(engagementId)
                .userId(userId)
                .amount(BigDecimal.valueOf(350))
                .date(LocalDate.of(2026, 3, 20))
                .build();

        when(this.incomeService.create(any())).thenReturn(response);

        this.mockMvc.perform(post("/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incomeId.toString()))
                .andExpect(jsonPath("$.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.amount").value(350))
                .andExpect(jsonPath("$.date").value("2026-03-20"));

        verify(this.incomeService).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenAmountIsZero() throws Exception {
        String requestBody = """
                {
                  "engagementId": "%s",
                  "userId": "%s",
                  "amount": 0,
                  "date": "2026-03-20"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        this.mockMvc.perform(post("/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.incomeService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenEngagementIdIsMissing() throws Exception {
        String requestBody = """
                {
                  "userId": "%s",
                  "amount": 350,
                  "date": "2026-03-20"
                }
                """.formatted(UUID.randomUUID());

        this.mockMvc.perform(post("/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.incomeService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenUserIdIsMissing() throws Exception {
        String requestBody = """
                {
                  "engagementId": "%s",
                  "amount": 350,
                  "date": "2026-03-20"
                }
                """.formatted(UUID.randomUUID());

        this.mockMvc.perform(post("/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.incomeService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenDateIsMissing() throws Exception {
        String requestBody = """
                {
                  "engagementId": "%s",
                  "userId": "%s",
                  "amount": 350
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        this.mockMvc.perform(post("/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.incomeService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenDateIsFuture() throws Exception {
        String requestBody = """
                {
                  "engagementId": "%s",
                  "userId": "%s",
                  "amount": 350,
                  "date": "2029-03-20"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        when(this.incomeService.create(any()))
                .thenThrow(new BadRequestException("Income date cannot be in the future"));

        this.mockMvc.perform(post("/incomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.incomeService).create(any());
    }
}