package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.*;
import es.upm.api.domain.model.report.NetIncomeBreakdownReport;
import es.upm.api.domain.model.report.Quarter;
import es.upm.api.domain.model.report.VatSummaryReport;
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

import static org.mockito.Mockito.*;
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
    void shouldReturnModel303() throws Exception {
        VatSummaryReport vatSummaryReport = new VatSummaryReport(
                new BigDecimal("300.00"),
                new BigDecimal("29.00"),
                new BigDecimal("75.00"),
                new BigDecimal("11.50"),
                new BigDecimal("4000.00"),
                new BigDecimal("840.00")
        );
        when(this.taxAgencyService.vatSummary(2026, Quarter.T2, 2, 3)).thenReturn(vatSummaryReport);

        this.mockMvc.perform(get("/tax-agency/models/303")
                        .param("year", "2026")
                        .param("quarter", "T2")
                        .param("from", "2")
                        .param("to", "3"))
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

        verify(this.taxAgencyService).vatSummary(2026, Quarter.T2, 2, 3);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnModel130() throws Exception {
        NetIncomeBreakdownReport netIncomeBreakdownReport = new NetIncomeBreakdownReport(
                new BigDecimal("300.00"),
                new BigDecimal("75.00"),
                new BigDecimal("1200.00"),
                new BigDecimal("10.00")
        );
        when(this.taxAgencyService.netIncomeBreakdown(2026, Quarter.T2, 3)).thenReturn(netIncomeBreakdownReport);

        this.mockMvc.perform(get("/tax-agency/models/130")
                        .param("year", "2026")
                        .param("quarter", "T2")
                        .param("to", "3"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "year": 2026,
                          "quarter": "T2",
                          "netIncomeBreakdown": {
                            "income": 300.00,
                            "currentExpenses": 75.00,
                            "investmentAmortization": 1200.00,
                            "withholdings": 10.00
                          }
                        }
                        """));

        verify(this.taxAgencyService).netIncomeBreakdown(2026, Quarter.T2, 3);
    }

    @Test
    @WithMockUser(roles = "admin")
    void shouldReturnReceivedBookZeroVat() throws Exception {
        Expense zeroVatExpense = this.buildExpense(LocalDate.of(2026, 4, 10), "2026", 3,
                "No Vat", "N10000000", "50.00", 0, null);
        when(this.taxAgencyService.invoiceReceiveBookZeroVat(2026, 2, 3)).thenReturn(List.of(zeroVatExpense));

        this.mockMvc.perform(get("/tax-agency/received-book-zero-vat")
                        .param("year", "2026")
                        .param("quarter", "T2")
                        .param("from", "2")
                        .param("to", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string("2026-3;T2;10/04/2026;10/04/2026;No Vat;N10000000;50,00;0%;0,00;50,00;OTROS"));

        verify(this.taxAgencyService).invoiceReceiveBookZeroVat(2026, 2, 3);
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
        return this.buildExpense(issueDate, null, null, supplierName, supplierIdentity, baseAmount, vatRate, null);
    }

    private Expense buildExpense(LocalDate issueDate, String supplierName, String supplierIdentity,
                                 String baseAmount, int vatRate, BigDecimal deductibleAmount) {
        return this.buildExpense(issueDate, null, null, supplierName, supplierIdentity, baseAmount, vatRate, deductibleAmount);
    }

    private Expense buildExpense(LocalDate issueDate, String series, Integer number, String supplierName, String supplierIdentity,
                                 String baseAmount, int vatRate, BigDecimal deductibleAmount) {
        return Expense.builder()
                .id(UUID.randomUUID())
                .series(series)
                .number(number)
                .recordedAt(issueDate.atTime(9, 0))
                .issueDate(issueDate)
                .baseAmount(new BigDecimal(baseAmount))
                .deductibleAmount(deductibleAmount)
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
