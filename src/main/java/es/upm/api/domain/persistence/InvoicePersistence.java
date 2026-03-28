package es.upm.api.domain.persistence;

import es.upm.api.domain.model.Invoice;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InvoicePersistence {
    void create(Invoice invoice);
    Invoice findByExpenseId(UUID expenseId);
    Invoice findByIncomeId(UUID incomeId);
}
