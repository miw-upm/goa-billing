package es.upm.api.domain.persistence;

import es.upm.api.domain.model.Invoice;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface InvoicePersistence {
    void create(Invoice invoice);
    Stream<Invoice> findAll();
    Stream<Invoice> findByEngagementId(UUID engagementId);
    Invoice findByExpenseId(UUID expenseId);
    Invoice findByIncomeId(UUID incomeId);
}
