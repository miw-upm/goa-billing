package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.BillingInfo;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.TaxCategory;
import es.upm.api.domain.model.report.VatSummary;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
    private static final NumberFormat AMOUNT = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-ES"));
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    static {
        AMOUNT.setGroupingUsed(false);
        AMOUNT.setMinimumFractionDigits(2);
        AMOUNT.setMaximumFractionDigits(2);
    }

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
                "12345678Z", "First Client", "100.00", "21", "21.00");
        Invoice second = this.buildInvoice(32, LocalDate.of(2026, 2, 20), LocalDate.of(2026, 2, 19),
                "87654321X", "Second Client", "200.00", "4", "8.00");
        String expected = String.join("\r\n",
                "2026-31;T1;%s;%s;First Client;12345678Z;%s;0,21;%s;%s".formatted(
                        DATE.format(LocalDate.of(2026, 1, 19)),
                        DATE.format(LocalDate.of(2026, 1, 20)),
                        AMOUNT.format(new BigDecimal("100.00")),
                        AMOUNT.format(new BigDecimal("21.00")),
                        AMOUNT.format(new BigDecimal("121.00"))
                ),
                "2026-32;T1;%s;%s;Second Client;87654321X;%s;0,04;%s;%s".formatted(
                        DATE.format(LocalDate.of(2026, 2, 19)),
                        DATE.format(LocalDate.of(2026, 2, 20)),
                        AMOUNT.format(new BigDecimal("200.00")),
                        AMOUNT.format(new BigDecimal("8.00")),
                        AMOUNT.format(new BigDecimal("208.00"))
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
    void shouldGenerateReceivedBookCsvWithContinuedReference() throws Exception {
        Expense firstT2Expense = this.buildExpense(LocalDate.of(2026, 4, 10),
                "Office Supplies", "B10000000", "100.00", 21);
        Expense secondT2Expense = this.buildExpense(LocalDate.of(2026, 5, 15),
                "Book Store", "B20000000", "200.00", 4);
        String expected = String.join("\r\n",
                "2;T2;%s;%s;Office Supplies;B10000000;%s;%s;0,21;%s;%s".formatted(
                        DATE.format(LocalDate.of(2026, 4, 10)),
                        DATE.format(LocalDate.of(2026, 4, 10)),
                        AMOUNT.format(new BigDecimal("100.00")),
                        AMOUNT.format(BigDecimal.ONE),
                        AMOUNT.format(new BigDecimal("21.00")),
                        AMOUNT.format(new BigDecimal("121.00"))
                ),
                "3;T2;%s;%s;Book Store;B20000000;%s;%s;0,04;%s;%s".formatted(
                        DATE.format(LocalDate.of(2026, 5, 15)),
                        DATE.format(LocalDate.of(2026, 5, 15)),
                        AMOUNT.format(new BigDecimal("200.00")),
                        AMOUNT.format(BigDecimal.ONE),
                        AMOUNT.format(new BigDecimal("8.00")),
                        AMOUNT.format(new BigDecimal("208.00"))
                )
        );
        when(this.taxAgencyService.countInvoiceReceiveBook(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(1);
        when(this.taxAgencyService.invoiceReceiveBook(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30)))
                .thenReturn(List.of(firstT2Expense, secondT2Expense));

        this.mockMvc.perform(get("/tax-agency/received-book")
                        .param("year", "2026")
                        .param("quarter", "T2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(expected));

        verify(this.taxAgencyService).countInvoiceReceiveBook(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
        verify(this.taxAgencyService).invoiceReceiveBook(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30));
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldGenerateReceivedBookCsvStartingAtOneInFirstQuarter() throws Exception {
        Expense expense = this.buildExpense(LocalDate.of(2026, 2, 10),
                "Office Supplies", "B10000000", "100.00", 21);
        String expected = "1;T1;%s;%s;Office Supplies;B10000000;%s;%s;0,21;%s;%s".formatted(
                DATE.format(LocalDate.of(2026, 2, 10)),
                DATE.format(LocalDate.of(2026, 2, 10)),
                AMOUNT.format(new BigDecimal("100.00")),
                AMOUNT.format(BigDecimal.ONE),
                AMOUNT.format(new BigDecimal("21.00")),
                AMOUNT.format(new BigDecimal("121.00"))
        );
        when(this.taxAgencyService.invoiceReceiveBook(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of(expense));

        this.mockMvc.perform(get("/tax-agency/received-book")
                        .param("year", "2026")
                        .param("quarter", "T1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(expected));

        verify(this.taxAgencyService, never()).countInvoiceReceiveBook(any(LocalDate.class), any(LocalDate.class));
        verify(this.taxAgencyService).invoiceReceiveBook(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnModel303() throws Exception {
        LocalDate fromDate = LocalDate.of(2026, 4, 1);
        LocalDate toDate = LocalDate.of(2026, 6, 30);
        VatSummary vatSummary = new VatSummary(
                new BigDecimal("300.00"),
                new BigDecimal("29.00"),
                new BigDecimal("75.00"),
                new BigDecimal("11.50"),
                new BigDecimal("4000.00"),
                new BigDecimal("840.00")
        );
        when(this.taxAgencyService.vatSummary(fromDate, toDate)).thenReturn(vatSummary);

        this.mockMvc.perform(get("/tax-agency/models/303")
                        .param("year", "2026")
                        .param("quarter", "T2"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "year": 2026,
                          "quarter": "T2",
                          "vatSummary": {
                            "invoiceIssuedBase": 300.00,
                            "invoiceIssuedVat": 29.00,
                            "invoiceReceivedCurrentBase": 75.00,
                            "invoiceReceivedCurrentVat": 11.50,
                            "invoiceReceivedInvestmentBase": 4000.00,
                            "invoiceReceivedInvestmentVat": 840.00
                          }
                        }
                        """));

        verify(this.taxAgencyService).vatSummary(fromDate, toDate);
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
                                 String identity, String fullName, String baseAmount, String vatRate, String vatAmount) {
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
                .vatRate(new BigDecimal(vatRate))
                .vatAmount(new BigDecimal(vatAmount))
                .build();
    }

    private Expense buildExpense(LocalDate issueDate, String supplierName, String supplierIdentity,
                                 String baseAmount, int vatRate) {
        return Expense.builder()
                .id(UUID.randomUUID())
                .issueDate(issueDate)
                .baseAmount(new BigDecimal(baseAmount))
                .vatRate(vatRate)
                .supplier(SupplierInfo.builder()
                        .name(supplierName)
                        .identity(supplierIdentity)
                        .build())
                .taxCategory(TaxCategory.OTROS)
                .depreciationRate(100)
                .build();
    }
}
