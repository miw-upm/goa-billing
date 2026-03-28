package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.persistence.InvoicePersistence;
import es.upm.api.infrastructure.mongodb.entities.InvoiceEntity;
import es.upm.api.infrastructure.mongodb.repositories.InvoiceRepository;
import org.springframework.stereotype.Repository;

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
}