package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.InvoiceFindCriteria;
import es.upm.api.domain.persistence.InvoicePersistence;
import es.upm.api.infrastructure.mongodb.entities.InvoiceEntity;
import es.upm.api.infrastructure.mongodb.repositories.InvoiceRepository;
import es.upm.miw.exception.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class InvoicePersistenceMongodb implements InvoicePersistence {

    public static final Sort DATE = Sort.by(Sort.Direction.DESC, "date");

    private final InvoiceRepository invoiceRepository;

    public InvoicePersistenceMongodb(InvoiceRepository invoiceRepository) {
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

        return this.invoiceRepository.save(invoiceEntity).toInvoice();
    }

    @Override
    public Invoice readById(UUID id) {
        return this.invoiceRepository.findById(id)
                .map(InvoiceEntity::toInvoice)
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
                .map(InvoiceEntity::toInvoice);
    }

    @Override
    public Invoice findByExpenseId(UUID expenseId) {
        InvoiceEntity invoiceEntity = this.invoiceRepository.findByExpensesId(expenseId);
        return invoiceEntity == null ? null : invoiceEntity.toInvoice();
    }

    @Override
    public Invoice findByIncomeId(UUID incomeId) {
        InvoiceEntity invoiceEntity = this.invoiceRepository.findByIncomesId(incomeId);
        return invoiceEntity == null ? null : invoiceEntity.toInvoice();
    }
}
