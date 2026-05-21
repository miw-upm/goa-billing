package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import es.upm.miw.exception.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class InvoiceAdapter implements InvoiceGateway {

    public static final Sort DATE = Sort.by(Sort.Direction.DESC, "date");

    private final InvoiceRepository invoiceRepository;

    public InvoiceAdapter(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public void create(Invoice invoice) {
        this.invoiceRepository.save(new InvoiceEntity(invoice));
    }

    @Override
    public Invoice update(UUID id, Invoice invoice) {
        InvoiceEntity invoiceEntity = this.invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invoice id: " + id));

        invoiceEntity.setEngagementId(invoice.getEngagementId());
        invoiceEntity.setDate(invoice.getDate());
        invoiceEntity.setExpenses(invoice.getExpenses());
        invoiceEntity.setIncomes(invoice.getIncomes());

        return this.invoiceRepository.save(invoiceEntity).toDomain();
    }

    @Override
    public Invoice readById(UUID id) {
        return this.invoiceRepository.findById(id)
                .map(InvoiceEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Invoice id: " + id));
    }

    @Override
    public Stream<Invoice> findAll(InvoiceFindCriteria criteria) {
        List<InvoiceEntity> result;

        if (criteria.isEmpty()) {
            result = this.invoiceRepository.findAll(DATE);
        } else if (criteria.getEngagementId() != null && criteria.getDate() != null) {
            result = this.invoiceRepository.findByEngagementIdAndDate(
                    criteria.getEngagementId(),
                    criteria.getDate(),
                    DATE
            );
        } else if (criteria.getEngagementId() != null) {
            result = this.invoiceRepository.findByEngagementId(criteria.getEngagementId(), DATE);
        } else {
            result = this.invoiceRepository.findByDate(criteria.getDate(), DATE);
        }

        return result.stream()
                .map(InvoiceEntity::toDomain);
    }

    @Override
    public Invoice findByExpenseId(UUID expenseId) {
        InvoiceEntity invoiceEntity = this.invoiceRepository.findByExpensesId(expenseId);
        return invoiceEntity == null ? null : invoiceEntity.toDomain();
    }

    @Override
    public Invoice findByIncomeId(UUID incomeId) {
        InvoiceEntity invoiceEntity = this.invoiceRepository.findByIncomesId(incomeId);
        return invoiceEntity == null ? null : invoiceEntity.toDomain();
    }
}
