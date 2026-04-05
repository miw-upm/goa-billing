package es.upm.api.domain.services;

import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.persistence.ExpensePersistence;
import es.upm.api.domain.persistence.IncomePersistence;
import es.upm.api.domain.persistence.InvoicePersistence;
import es.upm.api.domain.webclients.EngagementWebClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class InvoiceService {

    private final InvoicePersistence invoicePersistence;
    private final ExpensePersistence expensePersistence;
    private final IncomePersistence incomePersistence;
    private final EngagementWebClient engagementWebClient;

    public InvoiceService(InvoicePersistence invoicePersistence,
                          ExpensePersistence expensePersistence,
                          IncomePersistence incomePersistence,
                          EngagementWebClient engagementWebClient) {
        this.invoicePersistence = invoicePersistence;
        this.expensePersistence = expensePersistence;
        this.incomePersistence = incomePersistence;
        this.engagementWebClient = engagementWebClient;
    }

    public Invoice create(Invoice invoice) {
        invoice.setId(UUID.randomUUID());
        this.validateInvoice(invoice);
        this.invoicePersistence.create(invoice);
        return invoice;
    }

    public Invoice update(UUID id, Invoice invoice) {
        this.invoicePersistence.readById(id);
        invoice.setId(id);
        this.validateInvoice(invoice);
        return this.invoicePersistence.update(id, invoice);
    }

    public Invoice readById(UUID id) {
        return this.invoicePersistence.readById(id);
    }

    public Stream<Invoice> findAll(UUID engagementId) {
        if (engagementId == null) {
            return this.invoicePersistence.findAll();
        }
        this.engagementWebClient.readById(engagementId);
        return this.invoicePersistence.findByEngagementId(engagementId);
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
            Expense existingExpense = this.expensePersistence.readById(expense.getId());
            if (!invoice.getEngagementId().equals(existingExpense.getEngagementId())) {
                throw new BadRequestException("Expense does not belong to the invoice engagement");
            }
            Invoice assignedInvoice = this.invoicePersistence.findByExpenseId(expense.getId());
            if (assignedInvoice != null && !invoice.getId().equals(assignedInvoice.getId())) {
                throw new BadRequestException("Expense is already assigned to another invoice");
            }
            return existingExpense;
        }).toList();
    }

    private List<Income> validateIncomes(Invoice invoice) {
        return invoice.getIncomes().stream().map(income -> {
            Income existingIncome = this.incomePersistence.readById(income.getId());
            if (!invoice.getEngagementId().equals(existingIncome.getEngagementId())) {
                throw new BadRequestException("Income does not belong to the invoice engagement");
            }
            Invoice assignedInvoice = this.invoicePersistence.findByIncomeId(income.getId());
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
