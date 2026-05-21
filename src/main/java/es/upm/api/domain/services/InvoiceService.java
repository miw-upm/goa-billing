package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.model.InvoiceBreakdown;
import es.upm.api.domain.model.BreakdownItem;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.billing.IncomeGateway;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import es.upm.api.domain.ports.out.engagement.EngagementWebClient;
import es.upm.miw.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class InvoiceService {

    private static final BigDecimal VAT_RATE = new BigDecimal("0.21");

    private final InvoiceGateway invoiceGateway;
    private final ExpenseGateway expenseGateway;
    private final IncomeGateway incomeGateway;
    private final EngagementWebClient engagementWebClient;

    public InvoiceService(InvoiceGateway invoiceGateway,
                          ExpenseGateway expenseGateway,
                          IncomeGateway incomeGateway,
                          EngagementWebClient engagementWebClient) {
        this.invoiceGateway = invoiceGateway;
        this.expenseGateway = expenseGateway;
        this.incomeGateway = incomeGateway;
        this.engagementWebClient = engagementWebClient;
    }

    public Invoice create(Invoice invoice) {
        invoice.setId(UUID.randomUUID());
        this.validateInvoice(invoice);
        this.invoiceGateway.create(invoice);
        return invoice;
    }

    public Invoice update(UUID id, Invoice invoice) {
        this.invoiceGateway.readById(id);
        invoice.setId(id);
        this.validateInvoice(invoice);
        return this.invoiceGateway.update(id, invoice);
    }

    public Invoice readById(UUID id) {
        return this.invoiceGateway.readById(id);
    }

    public Stream<Invoice> findAll(InvoiceFindCriteria criteria) {
        if (criteria.getEngagementId() != null) {
            this.engagementWebClient.readById(criteria.getEngagementId());
        }
        return this.invoiceGateway.findAll(criteria);
    }

    public InvoiceBreakdown getInvoiceBreakdown(UUID id) {
        Invoice invoice = this.readById(id);
        if (invoice == null) {
            throw new BadRequestException("Invoice not found");
        }

        List<BreakdownItem> incomeBreakdownList = invoice.getIncomes().stream()
                .map(this::calculateIncomeBreakdown)
                .toList();

        List<BreakdownItem> expenseBreakdownList = invoice.getExpenses().stream()
                .map(this::calculateExpenseBreakdown)
                .toList();

        BigDecimal totalTaxableBase =
                incomeBreakdownList.stream()
                        .map(BreakdownItem::getTaxableBase)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .subtract(
                                expenseBreakdownList.stream()
                                        .map(BreakdownItem::getTaxableBase)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal totalVatAmount =
                incomeBreakdownList.stream()
                        .map(BreakdownItem::getVatAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .subtract(
                                expenseBreakdownList.stream()
                                        .map(BreakdownItem::getVatAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        BigDecimal totalAmount = totalTaxableBase.add(totalVatAmount);

        return InvoiceBreakdown.builder()
                .taxableBase(totalTaxableBase)
                .vatAmount(totalVatAmount)
                .totalAmount(totalAmount)
                .incomes(incomeBreakdownList)
                .expenses(expenseBreakdownList)
                .build();
    }

    protected BreakdownItem calculateIncomeBreakdown(Income income) {
        BigDecimal taxableBase = income.getAmount().divide(VAT_RATE.add(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = income.getAmount().subtract(taxableBase);
        return BreakdownItem.builder()
                .id(income.getId())
                .amountWithVat(income.getAmount())
                .taxableBase(taxableBase)
                .vatAmount(vatAmount)
                .build();
    }

    protected BreakdownItem calculateExpenseBreakdown(Expense expense) {
        BigDecimal taxableBase = expense.getAmount().divide(VAT_RATE.add(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        BigDecimal vatAmount = expense.getAmount().subtract(taxableBase);
        return BreakdownItem.builder()
                .id(expense.getId())
                .amountWithVat(expense.getAmount())
                .taxableBase(taxableBase)
                .vatAmount(vatAmount)
                .build();
    }

    private void validateInvoice(Invoice invoice) {
        if (invoice.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Invoice date cannot be in the future");
        }
        if (this.isNullOrEmpty(invoice.getExpenses()) && this.isNullOrEmpty(invoice.getIncomes())) {
            throw new BadRequestException("Invoice must contain at least one expense or one income");
        }

        this.engagementWebClient.readById(invoice.getEngagementId());
        invoice.setExpenses(this.validateExpenses(invoice));
        invoice.setIncomes(this.validateIncomes(invoice));
    }

    private List<Expense> validateExpenses(Invoice invoice) {
        return invoice.getExpenses().stream().map(expense -> {
            Expense existingExpense = this.expenseGateway.readById(expense.getId());
            if (!invoice.getEngagementId().equals(existingExpense.getEngagementId())) {
                throw new BadRequestException("Expense does not belong to the invoice engagement");
            }
            Invoice assignedInvoice = this.invoiceGateway.findByExpenseId(expense.getId());
            if (assignedInvoice != null && !invoice.getId().equals(assignedInvoice.getId())) {
                throw new BadRequestException("Expense is already assigned to another invoice");
            }
            return existingExpense;
        }).toList();
    }

    private List<Income> validateIncomes(Invoice invoice) {
        return invoice.getIncomes().stream().map(income -> {
            Income existingIncome = this.incomeGateway.readById(income.getId());
            if (!invoice.getEngagementId().equals(existingIncome.getEngagementId())) {
                throw new BadRequestException("Income does not belong to the invoice engagement");
            }
            Invoice assignedInvoice = this.invoiceGateway.findByIncomeId(income.getId());
            if (assignedInvoice != null && !invoice.getId().equals(assignedInvoice.getId())) {
                throw new BadRequestException("Income is already assigned to another invoice");
            }
            return existingIncome;
        }).toList();
    }

    private boolean isNullOrEmpty(List<?> items) {
        return items == null || items.isEmpty();
    }
}
