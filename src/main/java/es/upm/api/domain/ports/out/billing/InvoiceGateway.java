package es.upm.api.domain.ports.out.billing;

import es.upm.api.domain.model.InvoiceOld;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface InvoiceGateway {
    void create(InvoiceOld invoiceOld);
    InvoiceOld update(UUID id, InvoiceOld invoiceOld);
    InvoiceOld readById(UUID id);
    Stream<InvoiceOld> findAll(InvoiceFindCriteria criteria);
    InvoiceOld findByExpenseId(UUID expenseId);
    InvoiceOld findByIncomeId(UUID incomeId);
}
