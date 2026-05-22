package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.api.domain.services.InvoiceService;
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
        UUID invoiceId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String requestBody = """
                {
                  "billingInfo": {
                    "userId": "%s",
                    "fullName": "John Doe",
                    "identity": "12345678A",
                    "fullAddress": "Madrid"
                  },
                  "operationDate": "2026-03-20",
                  "series": "A",
                  "number": 1,
                  "baseAmount": 100,
                  "engagement": { "id": "%s" },
                  "payments": [
                    {
                      "id": "%s",
                      "engagement": { "id": "%s" },
                      "user": { "id": "%s" },
                      "amount": 100,
                      "method": "TRANSFER"
                    }
                  ],
                  "discounts": [10]
                }
                """.formatted(userId, engagementId, paymentId, engagementId, userId);

        Invoice response = this.buildInvoice(invoiceId, engagementId, userId, paymentId, "90", new BigDecimal("21"));
        when(this.invoiceService.create(any())).thenReturn(response);

        this.mockMvc.perform(post("/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.engagement.id").value(engagementId.toString()))
                .andExpect(jsonPath("$.billingInfo.userId").value(userId.toString()))
                .andExpect(jsonPath("$.payments[0].id").value(paymentId.toString()))
                .andExpect(jsonPath("$.baseAmount").value(90))
                .andExpect(jsonPath("$.vatRate").value(21))
                .andExpect(jsonPath("$.emissionDate").value("2026-03-20"));

        verify(this.invoiceService).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenInvalidInvoiceBody() throws Exception {
        UUID engagementId = UUID.randomUUID();
        String requestBody = """
                {
                  "operationDate": "2026-03-20",
                  "baseAmount": 100,
                  "engagement": { "id": "%s" },
                  "payments": []
                }
                """.formatted(engagementId);

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
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Invoice response = this.buildInvoice(invoiceId, engagementId, userId, paymentId, "90", new BigDecimal("21"));
        when(this.invoiceService.read(invoiceId)).thenReturn(response);

        this.mockMvc.perform(get("/invoices/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.engagement.id").value(engagementId.toString()))
                .andExpect(jsonPath("$.billingInfo.userId").value(userId.toString()))
                .andExpect(jsonPath("$.payments[0].id").value(paymentId.toString()))
                .andExpect(jsonPath("$.baseAmount").value(90))
                .andExpect(jsonPath("$.emissionDate").value("2026-03-20"));

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
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String requestBody = """
                {
                  "billingInfo": {
                    "userId": "%s",
                    "fullName": "John Doe",
                    "identity": "12345678A",
                    "fullAddress": "Madrid"
                  },
                  "operationDate": "2026-03-21",
                  "series": "B",
                  "number": 2,
                  "baseAmount": 120,
                  "engagement": { "id": "%s" },
                  "payments": [
                    {
                      "id": "%s",
                      "engagement": { "id": "%s" },
                      "user": { "id": "%s" },
                      "amount": 120,
                      "method": "CASH"
                    }
                  ],
                  "discounts": []
                }
                """.formatted(userId, engagementId, paymentId, engagementId, userId);

        Invoice response = this.buildInvoice(invoiceId, engagementId, userId, paymentId, "120", new BigDecimal("21"));
        response.setSeries("B");
        response.setNumber(2);
        response.getPayments().get(0).setMethod(PaymentMethod.CASH);
        when(this.invoiceService.update(eq(invoiceId), any())).thenReturn(response);

        this.mockMvc.perform(put("/invoices/{id}", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.series").value("B"))
                .andExpect(jsonPath("$.number").value(2))
                .andExpect(jsonPath("$.payments[0].method").value("CASH"))
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
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Invoice response = this.buildInvoice(invoiceId, engagementId, userId, paymentId, "90", new BigDecimal("21"));

        when(this.invoiceService.find(this.criteria)).thenReturn(Stream.of(response));

        this.mockMvc.perform(get("/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(invoiceId.toString()))
                .andExpect(jsonPath("$[0].engagement.id").value(engagementId.toString()))
                .andExpect(jsonPath("$[0].billingInfo.userId").value(userId.toString()))
                .andExpect(jsonPath("$[0].payments[0].id").value(paymentId.toString()))
                .andExpect(jsonPath("$[0].baseAmount").value(90));

        verify(this.invoiceService).find(this.criteria);
    }

    private Invoice buildInvoice(UUID invoiceId, UUID engagementId, UUID userId, UUID paymentId,
                                 String baseAmount, BigDecimal vatRate) {
        return Invoice.builder()
                .id(invoiceId)
                .billingInfo(BillingInfo.builder()
                        .userId(userId)
                        .fullName("John Doe")
                        .identity("12345678A")
                        .fullAddress("Madrid")
                        .build())
                .emissionDate(LocalDate.of(2026, 3, 20))
                .operationDate(LocalDate.of(2026, 3, 20))
                .series("A")
                .number(1)
                .baseAmount(new BigDecimal(baseAmount))
                .vatRate(vatRate)
                .engagement(EngagementSnapshot.builder().id(engagementId).build())
                .payments(List.of(Payment.builder()
                        .id(paymentId)
                        .engagement(EngagementSnapshot.builder().id(engagementId).build())
                        .user(UserSnapshot.builder().id(userId).build())
                        .amount(new BigDecimal(baseAmount))
                        .method(PaymentMethod.TRANSFER)
                        .date(LocalDate.of(2026, 3, 20))
                        .build()))
                .discounts(List.of(BigDecimal.TEN))
                .build();
    }
}

