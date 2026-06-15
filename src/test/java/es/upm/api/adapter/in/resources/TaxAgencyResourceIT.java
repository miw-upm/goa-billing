package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.services.TaxAgencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TaxAgencyResourceIT {
    private static final NumberFormat EUR = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES"));
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private TaxAgencyService taxAgencyService;

    @Test
    @WithMockUser(roles = "admin")
    void shouldGenerateInvoiceIssuedBookCsv() throws Exception {
        LocalDate fromDate = LocalDate.of(2026, 1, 1);
        LocalDate toDate = LocalDate.of(2026, 3, 31);
        Invoice first = this.buildInvoice(31, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 19),
                "11111111A", "First Client", "100.00", "21.00");
        Invoice second = this.buildInvoice(32, LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 19),
                "22222222B", "Second Client", "200.00", "42.00");
        String expected = String.join("\r\n",
                "2026-00031;T1;%s;%s;11111111A;First Client;%s;21;%s;%s".formatted(
                        DATE.format(LocalDate.of(2026, 1, 20)),
                        DATE.format(LocalDate.of(2026, 1, 19)),
                        EUR.format(new BigDecimal("100.00")),
                        EUR.format(new BigDecimal("21.00")),
                        EUR.format(new BigDecimal("121.00"))
                ),
                "2026-00032;T1;%s;%s;22222222B;Second Client;%s;21;%s;%s".formatted(
                        DATE.format(LocalDate.of(2026, 2, 20)),
                        DATE.format(LocalDate.of(2026, 2, 19)),
                        EUR.format(new BigDecimal("200.00")),
                        EUR.format(new BigDecimal("42.00")),
                        EUR.format(new BigDecimal("242.00"))
                )
        );
        when(this.taxAgencyService.invoiceIssuedBook(fromDate, toDate)).thenReturn(List.of(first, second));

        this.mockMvc.perform(get("/tax-agency/invoice-issued-book")
                        .param("year", "2026")
                        .param("quarter", "T1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(expected));

        verify(this.taxAgencyService).invoiceIssuedBook(fromDate, toDate);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnBadRequestForInvalidQuarter() throws Exception {
        this.mockMvc.perform(get("/tax-agency/invoice-issued-book")
                        .param("year", "2026")
                        .param("quarter", "Q1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(this.taxAgencyService);
    }

    private Invoice buildInvoice(int number, LocalDate emissionDate, LocalDate operationDate,
                                 String identity, String fullName, String baseAmount, String vatAmount) {
        return Invoice.builder()
                .id(UUID.randomUUID())
                .series("2026")
                .number(number)
                .emissionDate(emissionDate)
                .operationDate(operationDate)
                .billingInfo(BillingInfo.builder()
                        .userId(UUID.randomUUID())
                        .identity(identity)
                        .fullName(fullName)
                        .fullAddress("Madrid")
                        .build())
                .baseAmount(new BigDecimal(baseAmount))
                .vatRate(new BigDecimal("21"))
                .vatAmount(new BigDecimal(vatAmount))
                .build();
    }
}
