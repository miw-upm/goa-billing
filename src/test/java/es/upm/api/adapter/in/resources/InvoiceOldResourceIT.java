package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.*;
import es.upm.api.domain.model.InvoiceOld;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.services.InvoiceService;
import es.upm.miw.exception.BadRequestException;
import es.upm.miw.exception.NotFoundException;
import org.mockito.ArgumentCaptor;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class InvoiceOldResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvoiceService invoiceService;

    @Test
    @WithMockUser(roles = "admin")
    void shouldCreateInvoice() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        UUID incomeId = UUID.randomUUID();

        String requestBody = """
                {
                  "engagementId": "%s",
                  "date": "2026-03-21",
                  "expenseIds": ["%s"],
                  "incomeIds": ["%s"]
                }
                """.formatted(engagementId, expenseId, incomeId);

        InvoiceOld response = InvoiceOld.builder()
                .id(invoiceId)
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of(Expense.builder()
                        .id(expenseId)
                        .engagementId(engagementId)
                        .amount(BigDecimal.valueOf(25))
                        .date(LocalDate.of(2026, 3, 20))
                        .description("Taxi")
                        .build()))
                .incomes(List.of(Income.builder()
                        .id(incomeId)
                        .engagementId(engagementId)
                        .userId(UUID.randomUUID())
                        .amount(BigDecimal.valueOf(250))
                        .date(LocalDate.of(2026, 3, 20))
                        .build()))
                .build();

        when(this.invoiceService.create(any())).thenReturn(response);

        this.mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.date").value("2026-03-21"))
                .andExpect(jsonPath("$.expenses[0].id").value(expenseId.toString()))
                .andExpect(jsonPath("$.incomes[0].id").value(incomeId.toString()));

        verify(this.invoiceService).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldCreateInvoiceWithOnlyExpenses() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();

        String requestBody = """
                {
                  "engagementId": "%s",
                  "date": "2026-03-21",
                  "expenseIds": ["%s"],
                  "incomeIds": []
                }
                """.formatted(engagementId, expenseId);

        InvoiceOld response = InvoiceOld.builder()
                .id(invoiceId)
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of(Expense.builder()
                        .id(expenseId)
                        .engagementId(engagementId)
                        .amount(BigDecimal.valueOf(25))
                        .date(LocalDate.of(2026, 3, 20))
                        .description("Taxi")
                        .build()))
                .incomes(List.of())
                .build();

        when(this.invoiceService.create(any())).thenReturn(response);

        this.mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.expenses[0].id").value(expenseId.toString()))
                .andExpect(jsonPath("$.incomes").isArray())
                .andExpect(jsonPath("$.incomes").isEmpty());

        verify(this.invoiceService).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldCreateInvoiceWithOnlyIncomes() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID incomeId = UUID.randomUUID();

        String requestBody = """
                {
                  "engagementId": "%s",
                  "date": "2026-03-21",
                  "expenseIds": [],
                  "incomeIds": ["%s"]
                }
                """.formatted(engagementId, incomeId);

        InvoiceOld response = InvoiceOld.builder()
                .id(invoiceId)
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of())
                .incomes(List.of(Income.builder()
                        .id(incomeId)
                        .engagementId(engagementId)
                        .userId(UUID.randomUUID())
                        .amount(BigDecimal.valueOf(250))
                        .date(LocalDate.of(2026, 3, 20))
                        .build()))
                .build();

        when(this.invoiceService.create(any())).thenReturn(response);

        this.mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.expenses").isArray())
                .andExpect(jsonPath("$.expenses").isEmpty())
                .andExpect(jsonPath("$.incomes[0].id").value(incomeId.toString()));

        verify(this.invoiceService).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenEngagementIdIsMissing() throws Exception {
        String requestBody = """
                {
                  "date": "2026-03-21",
                  "expenseIds": ["%s"],
                  "incomeIds": ["%s"]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        this.mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.invoiceService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenExpenseIdsAndIncomeIdsAreEmpty() throws Exception {
        String requestBody = """
                {
                  "engagementId": "%s",
                  "date": "2026-03-21",
                  "expenseIds": [],
                  "incomeIds": []
                }
                """.formatted(UUID.randomUUID());

        this.mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.invoiceService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenDateIsFuture() throws Exception {
        String requestBody = """
                {
                  "engagementId": "%s",
                  "date": "2029-03-21",
                  "expenseIds": ["%s"],
                  "incomeIds": ["%s"]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        when(this.invoiceService.create(any()))
                .thenThrow(new BadRequestException("InvoiceOld date cannot be in the future"));

        this.mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.invoiceService).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldUpdateInvoice() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID incomeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        String requestBody = """
                {
                  "engagementId": "%s",
                  "date": "2026-03-25",
                  "expenseIds": [],
                  "incomeIds": ["%s"]
                }
                """.formatted(engagementId, incomeId);

        InvoiceOld response = InvoiceOld.builder()
                .id(invoiceId)
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 25))
                .expenses(List.of())
                .incomes(List.of(Income.builder()
                        .id(incomeId)
                        .engagementId(engagementId)
                        .userId(userId)
                        .amount(BigDecimal.valueOf(300))
                        .date(LocalDate.of(2026, 3, 24))
                        .build()))
                .build();

        when(this.invoiceService.update(any(), any())).thenReturn(response);

        this.mockMvc.perform(put("/invoices/{id}", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.date").value("2026-03-25"))
                .andExpect(jsonPath("$.expenses").isArray())
                .andExpect(jsonPath("$.expenses").isEmpty())
                .andExpect(jsonPath("$.incomes[0].id").value(incomeId.toString()));

        verify(this.invoiceService).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenUpdatingInvoiceWithEmptyItems() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagementId": "%s",
                  "date": "2026-03-25",
                  "expenseIds": [],
                  "incomeIds": []
                }
                """.formatted(UUID.randomUUID());

        this.mockMvc.perform(put("/invoices/{id}", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.invoiceService, never()).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenUpdatingInvoiceWithMissingEngagementId() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        String requestBody = """
                {
                  "date": "2026-03-25",
                  "expenseIds": ["%s"],
                  "incomeIds": []
                }
                """.formatted(UUID.randomUUID());

        this.mockMvc.perform(put("/invoices/{id}", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.invoiceService, never()).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenUpdatingInvoiceWithFutureDate() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagementId": "%s",
                  "date": "2029-03-21",
                  "expenseIds": ["%s"],
                  "incomeIds": []
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        when(this.invoiceService.update(any(), any()))
                .thenThrow(new BadRequestException("InvoiceOld date cannot be in the future"));

        this.mockMvc.perform(put("/invoices/{id}", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.invoiceService).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnNotFoundWhenUpdatingMissingInvoice() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagementId": "%s",
                  "date": "2026-03-25",
                  "expenseIds": ["%s"],
                  "incomeIds": []
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        when(this.invoiceService.update(any(), any()))
                .thenThrow(new NotFoundException("InvoiceOld id: " + invoiceId));

        this.mockMvc.perform(put("/invoices/{id}", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());

        verify(this.invoiceService).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindAllInvoices() throws Exception {
        InvoiceOld invoiceOldA = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(LocalDate.of(2026, 3, 20))
                .expenses(List.of())
                .incomes(List.of())
                .build();
        InvoiceOld invoiceOldB = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of())
                .incomes(List.of())
                .build();

        when(this.invoiceService.findAll(any(InvoiceFindCriteria.class))).thenReturn(Stream.of(invoiceOldA, invoiceOldB));

        this.mockMvc.perform(get("/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invoiceOldA.getId().toString()))
                .andExpect(jsonPath("$[1].id").value(invoiceOldB.getId().toString()));

        ArgumentCaptor<InvoiceFindCriteria> criteriaCaptor = ArgumentCaptor.forClass(InvoiceFindCriteria.class);
        verify(this.invoiceService).findAll(criteriaCaptor.capture());
        assertEquals(new InvoiceFindCriteria(null, null), criteriaCaptor.getValue());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReadInvoiceById() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();
        UUID incomeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        InvoiceOld invoiceOld = InvoiceOld.builder()
                .id(invoiceId)
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of(Expense.builder()
                        .id(expenseId)
                        .engagementId(engagementId)
                        .amount(BigDecimal.valueOf(25))
                        .date(LocalDate.of(2026, 3, 20))
                        .description("Taxi")
                        .build()))
                .incomes(List.of(Income.builder()
                        .id(incomeId)
                        .engagementId(engagementId)
                        .userId(userId)
                        .amount(BigDecimal.valueOf(250))
                        .date(LocalDate.of(2026, 3, 20))
                        .build()))
                .build();

        when(this.invoiceService.readById(invoiceId)).thenReturn(invoiceOld);

        this.mockMvc.perform(get("/invoices/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.date").value("2026-03-21"))
                .andExpect(jsonPath("$.expenses[0].id").value(expenseId.toString()))
                .andExpect(jsonPath("$.expenses[0].description").value("Taxi"))
                .andExpect(jsonPath("$.incomes[0].id").value(incomeId.toString()))
                .andExpect(jsonPath("$.incomes[0].userId").value(userId.toString()));

        verify(this.invoiceService).readById(invoiceId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnNotFoundWhenInvoiceDoesNotExist() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        when(this.invoiceService.readById(invoiceId))
                .thenThrow(new NotFoundException("InvoiceOld id: " + invoiceId));

        this.mockMvc.perform(get("/invoices/{id}", invoiceId))
                .andExpect(status().isNotFound());

        verify(this.invoiceService).readById(invoiceId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindInvoicesByEngagementId() throws Exception {
        UUID engagementId = UUID.randomUUID();
        InvoiceOld invoiceOldA = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 20))
                .expenses(List.of())
                .incomes(List.of())
                .build();
        InvoiceOld invoiceOldB = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of())
                .incomes(List.of())
                .build();

        when(this.invoiceService.findAll(any(InvoiceFindCriteria.class))).thenReturn(Stream.of(invoiceOldA, invoiceOldB));

        this.mockMvc.perform(get("/invoices").param("engagementId", engagementId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invoiceOldA.getId().toString()))
                .andExpect(jsonPath("$[0].engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$[1].id").value(invoiceOldB.getId().toString()))
                .andExpect(jsonPath("$[1].engagementId").value(engagementId.toString()));

        ArgumentCaptor<InvoiceFindCriteria> criteriaCaptor = ArgumentCaptor.forClass(InvoiceFindCriteria.class);
        verify(this.invoiceService).findAll(criteriaCaptor.capture());
        assertEquals(new InvoiceFindCriteria(engagementId, null), criteriaCaptor.getValue());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindInvoicesByDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 3, 21);
        InvoiceOld invoiceOldA = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(date)
                .expenses(List.of())
                .incomes(List.of())
                .build();
        InvoiceOld invoiceOldB = InvoiceOld.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(date)
                .expenses(List.of())
                .incomes(List.of())
                .build();

        when(this.invoiceService.findAll(any(InvoiceFindCriteria.class))).thenReturn(Stream.of(invoiceOldA, invoiceOldB));

        this.mockMvc.perform(get("/invoices").param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invoiceOldA.getId().toString()))
                .andExpect(jsonPath("$[0].date").value(date.toString()))
                .andExpect(jsonPath("$[1].id").value(invoiceOldB.getId().toString()))
                .andExpect(jsonPath("$[1].date").value(date.toString()));

        ArgumentCaptor<InvoiceFindCriteria> criteriaCaptor = ArgumentCaptor.forClass(InvoiceFindCriteria.class);
        verify(this.invoiceService).findAll(criteriaCaptor.capture());
        assertEquals(new InvoiceFindCriteria(null, date), criteriaCaptor.getValue());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenDateFilterIsInvalid() throws Exception {
        this.mockMvc.perform(get("/invoices").param("date", "2026/03/21"))
                .andExpect(status().isBadRequest());

        verify(this.invoiceService, never()).findAll(any(InvoiceFindCriteria.class));
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldGetInvoiceBreakdown() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID incomeId = UUID.randomUUID();
        UUID expenseId = UUID.randomUUID();

        InvoiceBreakdown breakdown = InvoiceBreakdown.builder()
                .taxableBase(new BigDecimal("178.14"))
                .vatAmount(new BigDecimal("47.35"))
                .totalAmount(new BigDecimal("225.49"))
                .incomes(List.of(
                        BreakdownItem.builder()
                                .id(incomeId)
                                .amountWithVat(new BigDecimal("250.99"))
                                .taxableBase(new BigDecimal("198.28"))
                                .vatAmount(new BigDecimal("52.71"))
                                .build()))
                .expenses(List.of(
                        BreakdownItem.builder()
                                .id(expenseId)
                                .amountWithVat(new BigDecimal("25.51"))
                                .taxableBase(new BigDecimal("20.15"))
                                .vatAmount(new BigDecimal("5.35"))
                                .build()))
                .build();

        when(this.invoiceService.getInvoiceBreakdown(invoiceId)).thenReturn(breakdown);

        this.mockMvc.perform(get("/invoices/" + invoiceId + "/breakdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taxableBase").value("178.14"))
                .andExpect(jsonPath("$.vatAmount").value("47.35"))
                .andExpect(jsonPath("$.totalAmount").value("225.49"))
                .andExpect(jsonPath("$.incomes[0].id").value(incomeId.toString()))
                .andExpect(jsonPath("$.incomes[0].amountWithVat").value("250.99"))
                .andExpect(jsonPath("$.incomes[0].taxableBase").value("198.28"))
                .andExpect(jsonPath("$.incomes[0].vatAmount").value("52.71"))
                .andExpect(jsonPath("$.expenses[0].id").value(expenseId.toString()))
                .andExpect(jsonPath("$.expenses[0].amountWithVat").value("25.51"))
                .andExpect(jsonPath("$.expenses[0].taxableBase").value("20.15"))
                .andExpect(jsonPath("$.expenses[0].vatAmount").value("5.35"));

        verify(this.invoiceService).getInvoiceBreakdown(invoiceId);
    }
}
