package es.upm.api.domain.persistence;

import es.upm.api.domain.model.Invoice;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoicePersistence {
    void create(Invoice invoice);
}