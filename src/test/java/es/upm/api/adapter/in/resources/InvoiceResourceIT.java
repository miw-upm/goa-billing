package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.creation.InvoiceCreationFromEngagement;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.services.InvoiceService;
import es.upm.miw.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class InvoiceResourceIT {

    private final InvoiceFindCriteria criteria = new InvoiceFindCriteria();
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private InvoiceService invoiceService;

    @Test
    @WithMockUser(roles = "admin")
    void shouldCreateInvoice() throws Exception {
        UUID userId = UUID.randomUUID();
        String requestBody = """
                {
                  "userId": "%s",
                  "concept": "Servicios",
                  "baseAmount": 100.00,
                  "discounts": [10.00]
                }
                """.formatted(userId);

        this.mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(this.invoiceService).create(invoiceCaptor.capture());
        assertEquals(userId, invoiceCaptor.getValue().getBillingInfo().getUserId());
        assertEquals("Servicios", invoiceCaptor.getValue().getConcept());
        assertEquals(new BigDecimal("100.00"), invoiceCaptor.getValue().getBaseAmount());
        assertEquals(1, invoiceCaptor.getValue().getDiscounts().size());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenInvalidCreateBody() throws Exception {
        UUID userId = UUID.randomUUID();
        String requestBody = """
                {
                  "userId": "%s",
                  "baseAmount": 100.00
                }
                """.formatted(userId);

        this.mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.invoiceService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReadInvoiceById() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice response = this.buildInvoice(invoiceId, userId, "100.00");
        when(this.invoiceService.read(invoiceId)).thenReturn(response);

        this.mockMvc.perform(get("/invoices/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.billingInfo.userId").value(userId.toString()))
                .andExpect(jsonPath("$.baseAmount").value(100));

        verify(this.invoiceService).read(invoiceId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnNotFoundWhenInvoiceDoesNotExist() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        when(this.invoiceService.read(invoiceId))
                .thenThrow(new NotFoundException("Invoice id: " + invoiceId));

        this.mockMvc.perform(get("/invoices/{id}", invoiceId))
                .andExpect(status().isNotFound());

        verify(this.invoiceService).read(invoiceId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldUpdateInvoice() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String requestBody = """
                {
                  "billingInfo": {
                    "userId": "%s",
                    "fullName": "John Doe",
                    "identity": "12345678A",
                    "fullAddress": "Madrid"
                  },
                  "operationDate": "2026-03-21",
                  "baseAmount": 120
                }
                """.formatted(userId);

        Invoice response = this.buildInvoice(invoiceId, userId, "120.00");
        when(this.invoiceService.update(eq(invoiceId), any())).thenReturn(response);

        this.mockMvc.perform(put("/invoices/{id}", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.baseAmount").value(120));

        verify(this.invoiceService).update(eq(invoiceId), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldDeleteInvoice() throws Exception {
        UUID invoiceId = UUID.randomUUID();

        this.mockMvc.perform(delete("/invoices/{id}", invoiceId))
                .andExpect(status().isOk());

        verify(this.invoiceService).delete(invoiceId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindInvoices() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Invoice response = this.buildInvoice(invoiceId, userId, "100.00");
        when(this.invoiceService.find(this.criteria)).thenReturn(Stream.of(response));

        this.mockMvc.perform(get("/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invoiceId.toString()))
                .andExpect(jsonPath("$[0].billingInfo.userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].baseAmount").value(100));

        verify(this.invoiceService).find(this.criteria);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindInvoicesByEngagementId() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String engagementIdPrefix = "2H60";
        Invoice response = this.buildInvoice(invoiceId, userId, "100.00");
        when(this.invoiceService.find(any(InvoiceFindCriteria.class))).thenReturn(Stream.of(response));

        this.mockMvc.perform(get("/invoices")
                        .param("engagementId", engagementIdPrefix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invoiceId.toString()));

        ArgumentCaptor<InvoiceFindCriteria> criteriaCaptor = ArgumentCaptor.forClass(InvoiceFindCriteria.class);
        verify(this.invoiceService).find(criteriaCaptor.capture());
        assertEquals(engagementIdPrefix, criteriaCaptor.getValue().getEngagementId());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldCreateInvoiceFromEngagement() throws Exception {
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagementId": "%s",
                  "closeEngagement": true,
                  "legalProcedures": [
                    {
                      "title": "Penal",
                      "budget": 300.00,
                      "legalTasks": ["Tarea 1", "Tarea 2"]
                    },
                    {
                      "title": "Civil",
                      "budget": 200.00,
                      "legalTasks": ["Tarea A"]
                    }
                  ],
                  "billingPercentages": [
                    {
                      "userId": "%s",
                      "percentage": 100.00
                    }
                  ]
                }
                """.formatted(engagementId, userId);

        this.mockMvc.perform(post("/invoices/from-engagement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        ArgumentCaptor<InvoiceCreationFromEngagement> creationCaptor =
                ArgumentCaptor.forClass(InvoiceCreationFromEngagement.class);
        verify(this.invoiceService).createFromEngagement(creationCaptor.capture());
        InvoiceCreationFromEngagement creation = creationCaptor.getValue();
        assertEquals(engagementId, creation.getEngagementId());
        assertEquals(Boolean.TRUE, creation.getCloseEngagement());
        assertEquals(2, creation.getLegalProcedures().size());
        assertEquals(new BigDecimal("500.00"), creation.totalBudget());
        assertEquals(1, creation.getBillingPercentages().size());
        assertEquals(userId, creation.getBillingPercentages().get(0).getUserId());
        assertEquals(new BigDecimal("100.00"), creation.getBillingPercentages().get(0).getPercentage());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldViewInvoicePdf() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        byte[] pdf = "%PDF-1.4".getBytes(StandardCharsets.UTF_8);
        when(this.invoiceService.generatePdf(invoiceId)).thenReturn(pdf);

        this.mockMvc.perform(get("/invoices/{id}/view", invoiceId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdf));

        verify(this.invoiceService).generatePdf(invoiceId);
    }

    private Invoice buildInvoice(UUID invoiceId, UUID userId, String baseAmount) {
        return Invoice.builder()
                .id(invoiceId)
                .concept("Servicios")
                .billingInfo(BillingInfo.builder()
                        .userId(userId)
                        .fullName("John Doe")
                        .identity("12345678A")
                        .fullAddress("Madrid")
                        .build())
                .emissionDate(LocalDate.of(2026, 3, 20))
                .operationDate(LocalDate.of(2026, 3, 20))
                .baseAmount(new BigDecimal(baseAmount))
                .build();
    }
}
