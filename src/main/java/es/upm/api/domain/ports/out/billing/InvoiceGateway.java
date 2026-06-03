package es.upm.api.domain.ports.out.billing;

import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface InvoiceGateway {
    Invoice create(Invoice invoice);

    Invoice update(UUID id, Invoice invoice);

    Invoice read(UUID id);

    Invoice read(String series, Integer number);

    void delete(UUID id);

    Stream<Invoice> find(InvoiceFindCriteria criteria);

    Optional<Invoice> findById(UUID id);

    Optional<Integer> findLastNumber(String series);
}
