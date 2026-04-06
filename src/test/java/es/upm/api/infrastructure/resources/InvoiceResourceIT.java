package es.upm.api.infrastructure.resources;

import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.exceptions.NotFoundException;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.InvoiceFindCriteria;
import es.upm.api.domain.services.InvoiceService;
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
class InvoiceResourceIT {

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

        Invoice response = Invoice.builder()
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

        Invoice response = Invoice.builder()
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

        Invoice response = Invoice.builder()
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
                .thenThrow(new BadRequestException("Invoice date cannot be in the future"));

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

        Invoice response = Invoice.builder()
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
                .thenThrow(new BadRequestException("Invoice date cannot be in the future"));

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
                .thenThrow(new NotFoundException("Invoice id: " + invoiceId));

        this.mockMvc.perform(put("/invoices/{id}", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not Found Exception. Invoice id: " + invoiceId));

        verify(this.invoiceService).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindAllInvoices() throws Exception {
        Invoice invoiceA = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(LocalDate.of(2026, 3, 20))
                .expenses(List.of())
                .incomes(List.of())
                .build();
        Invoice invoiceB = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of())
                .incomes(List.of())
                .build();

        when(this.invoiceService.findAll(any(InvoiceFindCriteria.class))).thenReturn(Stream.of(invoiceA, invoiceB));

        this.mockMvc.perform(get("/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invoiceA.getId().toString()))
                .andExpect(jsonPath("$[1].id").value(invoiceB.getId().toString()));

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

        Invoice invoice = Invoice.builder()
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

        when(this.invoiceService.readById(invoiceId)).thenReturn(invoice);

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
                .thenThrow(new NotFoundException("Invoice id: " + invoiceId));

        this.mockMvc.perform(get("/invoices/{id}", invoiceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not Found Exception. Invoice id: " + invoiceId));

        verify(this.invoiceService).readById(invoiceId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindInvoicesByEngagementId() throws Exception {
        UUID engagementId = UUID.randomUUID();
        Invoice invoiceA = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 20))
                .expenses(List.of())
                .incomes(List.of())
                .build();
        Invoice invoiceB = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(engagementId)
                .date(LocalDate.of(2026, 3, 21))
                .expenses(List.of())
                .incomes(List.of())
                .build();

        when(this.invoiceService.findAll(any(InvoiceFindCriteria.class))).thenReturn(Stream.of(invoiceA, invoiceB));

        this.mockMvc.perform(get("/invoices").param("engagementId", engagementId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invoiceA.getId().toString()))
                .andExpect(jsonPath("$[0].engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$[1].id").value(invoiceB.getId().toString()))
                .andExpect(jsonPath("$[1].engagementId").value(engagementId.toString()));

        ArgumentCaptor<InvoiceFindCriteria> criteriaCaptor = ArgumentCaptor.forClass(InvoiceFindCriteria.class);
        verify(this.invoiceService).findAll(criteriaCaptor.capture());
        assertEquals(new InvoiceFindCriteria(engagementId, null), criteriaCaptor.getValue());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindInvoicesByDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 3, 21);
        Invoice invoiceA = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(date)
                .expenses(List.of())
                .incomes(List.of())
                .build();
        Invoice invoiceB = Invoice.builder()
                .id(UUID.randomUUID())
                .engagementId(UUID.randomUUID())
                .date(date)
                .expenses(List.of())
                .incomes(List.of())
                .build();

        when(this.invoiceService.findAll(any(InvoiceFindCriteria.class))).thenReturn(Stream.of(invoiceA, invoiceB));

        this.mockMvc.perform(get("/invoices").param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invoiceA.getId().toString()))
                .andExpect(jsonPath("$[0].date").value(date.toString()))
                .andExpect(jsonPath("$[1].id").value(invoiceB.getId().toString()))
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
}
