package es.upm.api.infrastructure.resources;

import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.exceptions.NotFoundException;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.IncomeFindCriteria;
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
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class IncomeResourceIT {

    @Test
    @WithMockUser(roles = "admin")
    void shouldReadIncomeById() throws Exception {
        UUID incomeId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Income response = Income.builder()
                .id(incomeId)
                .engagementId(engagementId)
                .userId(userId)
                .amount(BigDecimal.valueOf(350))
                .date(LocalDate.of(2026, 3, 20))
                .build();

        when(this.incomeService.readById(incomeId)).thenReturn(response);

        this.mockMvc.perform(get("/incomes/{id}", incomeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incomeId.toString()))
                .andExpect(jsonPath("$.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.amount").value(350))
                .andExpect(jsonPath("$.date").value("2026-03-20"));

        verify(this.incomeService).readById(incomeId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnNotFoundWhenIncomeDoesNotExist() throws Exception {
        UUID incomeId = UUID.randomUUID();
        when(this.incomeService.readById(incomeId))
                .thenThrow(new NotFoundException("Income id: " + incomeId));

        this.mockMvc.perform(get("/incomes/{id}", incomeId))
                .andExpect(status().isNotFound());

        verify(this.incomeService).readById(incomeId);
    }

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

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindAllIncomes() throws Exception {
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Income incomeA = Income.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .userId(userId)
                .amount(BigDecimal.valueOf(350))
                .date(LocalDate.of(2026, 3, 20))
                .build();

        Income incomeB = Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(200))
                .date(LocalDate.of(2026, 3, 19))
                .build();

        when(this.incomeService.findAll(any(IncomeFindCriteria.class))).thenReturn(Stream.of(incomeA, incomeB));

        this.mockMvc.perform(get("/incomes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(incomeA.getId().toString()))
                .andExpect(jsonPath("$[1].id").value(incomeB.getId().toString()));

        verify(this.incomeService).findAll(new IncomeFindCriteria(null, null));
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnEmptyArrayWhenNoIncomesExist() throws Exception {
       when(this.incomeService.findAll(any(IncomeFindCriteria.class))).thenReturn(Stream.empty());
        this.mockMvc.perform(get("/incomes"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(this.incomeService).findAll(argThat(criteria ->
                criteria.getEngagementId() == null && criteria.getDate() == null
        ));
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldUpdateIncome() throws Exception {
        UUID incomeId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String requestBody = """
                {
                  "engagementId": "%s",
                  "userId": "%s",
                  "amount": 500,
                  "date": "2026-03-25"
                }
                """.formatted(engagementId, userId);

        Income response = Income.builder()
                .id(incomeId)
                .engagementId(engagementId)
                .userId(userId)
                .amount(BigDecimal.valueOf(500))
                .date(LocalDate.of(2026, 3, 25))
                .build();

        when(this.incomeService.update(eq(incomeId), any())).thenReturn(response);

        this.mockMvc.perform(put("/incomes/" + incomeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incomeId.toString()))
                .andExpect(jsonPath("$.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.date").value("2026-03-25"));

        verify(this.incomeService).update(eq(incomeId), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenUpdateDateIsFuture() throws Exception {
        UUID incomeId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagementId": "%s",
                  "userId": "%s",
                  "amount": 500,
                  "date": "2029-03-25"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        when(this.incomeService.update(eq(incomeId), any()))
                .thenThrow(new BadRequestException("Income date cannot be in the future"));

        this.mockMvc.perform(put("/incomes/" + incomeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.incomeService).update(eq(incomeId), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnNotFoundWhenUpdateIncomeDoesNotExist() throws Exception {
        UUID incomeId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagementId": "%s",
                  "userId": "%s",
                  "amount": 500,
                  "date": "2026-03-25"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        when(this.incomeService.update(eq(incomeId), any()))
                .thenThrow(new es.upm.api.domain.exceptions.NotFoundException("Income not found: " + incomeId));

        this.mockMvc.perform(put("/incomes/" + incomeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());

        verify(this.incomeService).update(eq(incomeId), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindIncomesByEngagementId() throws Exception {
        UUID engagementId = UUID.randomUUID();

        Income incomeA = Income.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(350))
                .date(LocalDate.of(2026, 3, 20))
                .build();

        Income incomeB = Income.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(200))
                .date(LocalDate.of(2026, 3, 19))
                .build();

        when(this.incomeService.findAll(any(IncomeFindCriteria.class))).thenReturn(Stream.of(incomeA, incomeB));

        this.mockMvc.perform(get("/incomes").param("engagementId", engagementId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$[1].engagementId").value(engagementId.toString()));

        verify(this.incomeService).findAll(new IncomeFindCriteria(engagementId, null));
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnNotFoundWhenEngagementIdIsInvalidInFilter() throws Exception {
        UUID engagementId = UUID.randomUUID();
        IncomeFindCriteria criteria = new IncomeFindCriteria(engagementId, null);

        when(this.incomeService.findAll(criteria))
                .thenThrow(new NotFoundException("Engagement not found: " + engagementId));

        this.mockMvc.perform(get("/incomes").param("engagementId", engagementId.toString()))
                .andExpect(status().isNotFound());

        verify(this.incomeService).findAll(criteria);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindIncomesByDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 3, 20);

        Income incomeA = Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(350))
                .date(date)
                .build();

        Income incomeB = Income.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(200))
                .date(date)
                .build();

        when(this.incomeService.findAll(any(IncomeFindCriteria.class))).thenReturn(Stream.of(incomeA, incomeB));

        this.mockMvc.perform(get("/incomes").param("date", "2026-03-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-03-20"))
                .andExpect(jsonPath("$[1].date").value("2026-03-20"));

        verify(this.incomeService).findAll(new IncomeFindCriteria(null, date));
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindIncomesByEngagementIdAndDate() throws Exception {
        UUID engagementId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 3, 20);

        Income income = Income.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(350))
                .date(date)
                .build();

        when(this.incomeService.findAll(any(IncomeFindCriteria.class))).thenReturn(Stream.of(income));

        this.mockMvc.perform(get("/incomes")
                        .param("engagementId", engagementId.toString())
                        .param("date", "2026-03-20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$[0].date").value("2026-03-20"));

        verify(this.incomeService).findAll(new IncomeFindCriteria(engagementId, date));
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenDateFilterIsInvalid() throws Exception {
        this.mockMvc.perform(get("/incomes").param("date", "20-03-2026"))
                .andExpect(status().isBadRequest());

        verify(this.incomeService, never()).findAll(any());
    }
}