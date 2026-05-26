package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    private final InvoiceFindCriteria criteria = new InvoiceFindCriteria();

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
        assertEquals("Servicios", invoiceCaptor.getValue().getBillingInfo().getConcept());
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
    void shouldCreateInvoicesFromPayments() throws Exception {
        UUID engagementId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagementId": "%s",
                  "discounts": [10.00, 5.00]
                }
                """.formatted(engagementId);

        this.mockMvc.perform(post("/invoices/from-payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(this.invoiceService).createFromPayments(
                engagementId,
                java.util.List.of(new BigDecimal("10.00"), new BigDecimal("5.00"))
        );
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldCreateInvoiceFromEngagement() throws Exception {
        UUID engagementId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagementId": "%s",
                  "totalBaseAmount": 500.00,
                  "concept": "Provisión inicial"
                }
                """.formatted(engagementId);

        this.mockMvc.perform(post("/invoices/from-engagement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(this.invoiceService).createFromEngagement(
                engagementId,
                new BigDecimal("500.00"),
                "Provisión inicial"
        );
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
                .billingInfo(BillingInfo.builder()
                        .userId(userId)
                        .fullName("John Doe")
                        .identity("12345678A")
                        .fullAddress("Madrid")
                        .concept("Servicios")
                        .build())
                .emissionDate(LocalDate.of(2026, 3, 20))
                .operationDate(LocalDate.of(2026, 3, 20))
                .baseAmount(new BigDecimal(baseAmount))
                .build();
    }
}
