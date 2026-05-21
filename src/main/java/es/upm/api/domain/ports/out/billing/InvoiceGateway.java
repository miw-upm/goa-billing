package es.upm.api.domain.ports.out.billing;

import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface InvoiceGateway {
    void create(Invoice invoice);
    Invoice update(UUID id, Invoice invoice);
    Invoice readById(UUID id);
    Stream<Invoice> findAll(InvoiceFindCriteria criteria);
    Invoice findByExpenseId(UUID expenseId);
    Invoice findByIncomeId(UUID incomeId);
}
