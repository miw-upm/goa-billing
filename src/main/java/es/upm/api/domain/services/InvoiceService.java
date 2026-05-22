package es.upm.api.domain.services;

import es.upm.api.domain.model.*;
import es.upm.api.domain.model.InvoiceOld;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.billing.IncomeGateway;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import es.upm.api.adapter.out.engagement.feign.EngagementWebClient;
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

    public InvoiceOld create(InvoiceOld invoiceOld) {
        invoiceOld.setId(UUID.randomUUID());
        this.validateInvoice(invoiceOld);
        this.invoiceGateway.create(invoiceOld);
        return invoiceOld;
    }

    public InvoiceOld update(UUID id, InvoiceOld invoiceOld) {
        this.invoiceGateway.readById(id);
        invoiceOld.setId(id);
        this.validateInvoice(invoiceOld);
        return this.invoiceGateway.update(id, invoiceOld);
    }

    public InvoiceOld readById(UUID id) {
        return this.invoiceGateway.readById(id);
    }

    public Stream<InvoiceOld> findAll(InvoiceFindCriteria criteria) {
        if (criteria.getEngagementId() != null) {
            this.engagementWebClient.readById(criteria.getEngagementId());
        }
        return this.invoiceGateway.findAll(criteria);
    }

    public InvoiceBreakdown getInvoiceBreakdown(UUID id) {
        InvoiceOld invoiceOld = this.readById(id);
        if (invoiceOld == null) {
            throw new BadRequestException("InvoiceOld not found");
        }

        List<BreakdownItem> incomeBreakdownList = invoiceOld.getIncomes().stream()
                .map(this::calculateIncomeBreakdown)
                .toList();

        List<BreakdownItem> expenseBreakdownList = invoiceOld.getExpenses().stream()
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

    private void validateInvoice(InvoiceOld invoiceOld) {
        if (invoiceOld.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("InvoiceOld date cannot be in the future");
        }
        if (this.isNullOrEmpty(invoiceOld.getExpenses()) && this.isNullOrEmpty(invoiceOld.getIncomes())) {
            throw new BadRequestException("InvoiceOld must contain at least one expense or one income");
        }

        this.engagementWebClient.readById(invoiceOld.getEngagementId());
        invoiceOld.setExpenses(this.validateExpenses(invoiceOld));
        invoiceOld.setIncomes(this.validateIncomes(invoiceOld));
    }

    private List<Expense> validateExpenses(InvoiceOld invoiceOld) {
        return invoiceOld.getExpenses().stream().map(expense -> {
            Expense existingExpense = this.expenseGateway.readById(expense.getId());
            if (!invoiceOld.getEngagementId().equals(existingExpense.getEngagementId())) {
                throw new BadRequestException("Expense does not belong to the invoiceOld engagement");
            }
            InvoiceOld assignedInvoiceOld = this.invoiceGateway.findByExpenseId(expense.getId());
            if (assignedInvoiceOld != null && !invoiceOld.getId().equals(assignedInvoiceOld.getId())) {
                throw new BadRequestException("Expense is already assigned to another invoiceOld");
            }
            return existingExpense;
        }).toList();
    }

    private List<Income> validateIncomes(InvoiceOld invoiceOld) {
        return invoiceOld.getIncomes().stream().map(income -> {
            Income existingIncome = this.incomeGateway.readById(income.getId());
            if (!invoiceOld.getEngagementId().equals(existingIncome.getEngagementId())) {
                throw new BadRequestException("Income does not belong to the invoiceOld engagement");
            }
            InvoiceOld assignedInvoiceOld = this.invoiceGateway.findByIncomeId(income.getId());
            if (assignedInvoiceOld != null && !invoiceOld.getId().equals(assignedInvoiceOld.getId())) {
                throw new BadRequestException("Income is already assigned to another invoiceOld");
            }
            return existingIncome;
        }).toList();
    }

    private boolean isNullOrEmpty(List<?> items) {
        return items == null || items.isEmpty();
    }
}
