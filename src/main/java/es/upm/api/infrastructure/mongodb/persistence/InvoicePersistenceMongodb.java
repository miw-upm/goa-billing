package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.exceptions.NotFoundException;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.persistence.InvoicePersistence;
import es.upm.api.infrastructure.mongodb.entities.InvoiceEntity;
import es.upm.api.infrastructure.mongodb.repositories.InvoiceRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class InvoicePersistenceMongodb implements InvoicePersistence {

    private final InvoiceRepository invoiceRepository;

    public InvoicePersistenceMongodb(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public void create(Invoice invoice) {
        this.invoiceRepository.save(new InvoiceEntity(invoice));
    }

    @Override
    public Invoice readById(UUID id) {
        return this.invoiceRepository.findById(id)
                .map(InvoiceEntity::toInvoice)
                .orElseThrow(() -> new NotFoundException("Invoice id: " + id));
    }

    @Override
    public Stream<Invoice> findAll() {
        return this.invoiceRepository.findAll().stream()
                .map(InvoiceEntity::toInvoice);
    }

    @Override
    public Stream<Invoice> findByEngagementId(UUID engagementId) {
        return this.invoiceRepository.findByEngagementId(engagementId).stream()
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
