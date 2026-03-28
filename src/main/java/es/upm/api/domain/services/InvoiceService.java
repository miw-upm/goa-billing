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
        if (invoice.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Invoice date cannot be in the future");
        }
        if (this.isNullOrEmpty(invoice.getExpenses()) && this.isNullOrEmpty(invoice.getIncomes())) {
            throw new BadRequestException("Invoice must contain at least one expense or one income");
        }

        invoice.setId(UUID.randomUUID());
        this.engagementWebClient.readById(invoice.getEngagementId());
        this.validateExpenses(invoice);
        this.validateIncomes(invoice);
        this.invoicePersistence.create(invoice);
        return invoice;
    }

    private void validateExpenses(Invoice invoice) {
        for (Expense expense : invoice.getExpenses()) {
            Expense existingExpense = this.expensePersistence.readById(expense.getId());
            if (!invoice.getEngagementId().equals(existingExpense.getEngagementId())) {
                throw new BadRequestException("Expense does not belong to the invoice engagement");
            }
            if (this.invoicePersistence.findByExpenseId(expense.getId()) != null) {
                throw new BadRequestException("Expense is already assigned to another invoice");
            }
        }
    }

    private void validateIncomes(Invoice invoice) {
        for (Income income : invoice.getIncomes()) {
            Income existingIncome = this.incomePersistence.readById(income.getId());
            if (!invoice.getEngagementId().equals(existingIncome.getEngagementId())) {
                throw new BadRequestException("Income does not belong to the invoice engagement");
            }
            if (this.invoicePersistence.findByIncomeId(income.getId()) != null) {
                throw new BadRequestException("Income is already assigned to another invoice");
            }
        }
    }

    private boolean isNullOrEmpty(List<?> items) {
        return items == null || items.isEmpty();
    }
}
