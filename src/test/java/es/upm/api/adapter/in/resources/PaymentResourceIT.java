package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.PaymentMethod;
import es.upm.api.domain.model.criteria.PaymentFindCriteria;
import es.upm.api.domain.model.external.EngagementSnapshot;
import es.upm.api.domain.model.external.UserSnapshot;
import es.upm.api.domain.services.PaymentService;
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
class PaymentResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private final PaymentFindCriteria criteria = new PaymentFindCriteria();

    @Test
    @WithMockUser(roles = "admin")
    void shouldCreatePayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagement": { "engagementId": "%s" },
                  "user": { "id": "%s" },
                  "amount": 350,
                  "method": "TRANSFER"
                }
                """.formatted(engagementId, userId);

        Payment response = this.buildPayment(paymentId, engagementId, userId, "350", PaymentMethod.TRANSFER, LocalDate.of(2026, 3, 20));
        when(this.paymentService.create(any())).thenReturn(response);

        this.mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.engagement.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.user.id").value(userId.toString()))
                .andExpect(jsonPath("$.amount").value(350))
                .andExpect(jsonPath("$.method").value("TRANSFER"))
                .andExpect(jsonPath("$.date").value("2026-03-20"));

        verify(this.paymentService).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestWhenInvalidPaymentBody() throws Exception {
        String requestBody = """
                {
                  "engagement": { "engagementId": "%s" },
                  "user": { "id": "%s" },
                  "amount": 0,
                  "method": "TRANSFER"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        this.mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verify(this.paymentService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReadPaymentById() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Payment response = this.buildPayment(paymentId, engagementId, userId, "350", PaymentMethod.BIZUM, LocalDate.of(2026, 3, 20));
        when(this.paymentService.read(paymentId)).thenReturn(response);

        this.mockMvc.perform(get("/payments/{id}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.engagement.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.user.id").value(userId.toString()))
                .andExpect(jsonPath("$.amount").value(350))
                .andExpect(jsonPath("$.method").value("BIZUM"))
                .andExpect(jsonPath("$.date").value("2026-03-20"));

        verify(this.paymentService).read(paymentId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnNotFoundWhenPaymentDoesNotExist() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(this.paymentService.read(paymentId))
                .thenThrow(new NotFoundException("Payment id: " + paymentId));

        this.mockMvc.perform(get("/payments/{id}", paymentId))
                .andExpect(status().isNotFound());

        verify(this.paymentService).read(paymentId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldUpdatePayment() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String requestBody = """
                {
                  "engagement": { "engagementId": "%s" },
                  "user": { "id": "%s" },
                  "amount": 500,
                  "method": "CASH"
                }
                """.formatted(engagementId, userId);

        Payment response = this.buildPayment(paymentId, engagementId, userId, "500", PaymentMethod.CASH, LocalDate.of(2026, 3, 21));
        when(this.paymentService.update(eq(paymentId), any())).thenReturn(response);

        this.mockMvc.perform(put("/payments/{id}", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.engagement.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$.user.id").value(userId.toString()))
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.method").value("CASH"))
                .andExpect(jsonPath("$.date").value("2026-03-21"));

        verify(this.paymentService).update(eq(paymentId), any());
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldDeletePayment() throws Exception {
        UUID paymentId = UUID.randomUUID();

        this.mockMvc.perform(delete("/payments/{id}", paymentId))
                .andExpect(status().isOk());

        verify(this.paymentService).delete(paymentId);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldFindPayments() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Payment response = this.buildPayment(paymentId, engagementId, userId, "200", PaymentMethod.OTHER, LocalDate.of(2026, 3, 19));

        when(this.paymentService.find(this.criteria)).thenReturn(Stream.of(response));

        this.mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(paymentId.toString()))
                .andExpect(jsonPath("$[0].engagement.engagementId").value(engagementId.toString()))
                .andExpect(jsonPath("$[0].user.id").value(userId.toString()))
                .andExpect(jsonPath("$[0].amount").value(200))
                .andExpect(jsonPath("$[0].method").value("OTHER"))
                .andExpect(jsonPath("$[0].date").value("2026-03-19"));

        verify(this.paymentService).find(this.criteria);
    }

    private Payment buildPayment(UUID paymentId, UUID engagementId, UUID userId, String amount,
                                 PaymentMethod method, LocalDate date) {
        return Payment.builder()
                .id(paymentId)
                .engagement(EngagementSnapshot.builder().engagementId(engagementId).build())
                .user(UserSnapshot.builder().id(userId).build())
                .amount(new BigDecimal(amount))
                .method(method)
                .date(date)
                .build();
    }
}
