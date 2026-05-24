package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import es.upm.miw.exception.NotFoundException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class InvoiceAdapter implements InvoiceGateway {
    public static final int FIRST_SERIES_NUMBER  = 30;
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

        invoiceEntity.setBillingInfo(invoice.getBillingInfo());
        invoiceEntity.setEmissionDate(invoice.getEmissionDate());
        invoiceEntity.setOperationDate(invoice.getOperationDate());
        invoiceEntity.setSeries(invoice.getSeries());
        invoiceEntity.setNumber(invoice.getNumber());
        invoiceEntity.setBaseAmount(invoice.getBaseAmount());
        invoiceEntity.setVatRate(invoice.getVatRate());
        invoiceEntity.setEngagementId(invoice.getEngagement() == null ? null : invoice.getEngagement().getId());
        invoiceEntity.setPayments(invoice.getPayments());
        invoiceEntity.setDiscounts(invoice.getDiscounts());
        invoiceEntity.setPdfPath(invoice.getPdfPath());
        invoiceEntity.setRectification(invoice.getRectification());

        return this.invoiceRepository.save(invoiceEntity).toDomain();
    }

    @Override
    public Invoice read(UUID id) {
        return this.invoiceRepository.findById(id)
                .map(InvoiceEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Invoice id: " + id));
    }

    @Override
    public void delete(UUID id) {
        InvoiceEntity invoiceEntity = this.invoiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Invoice id: " + id));
        this.invoiceRepository.delete(invoiceEntity);
    }

    @Override
    public Stream<Invoice> find(InvoiceFindCriteria criteria) {
        List<InvoiceEntity> result;

        if (criteria.isEmpty() || criteria.getFromDate() == null) {
            result = this.invoiceRepository.findAllByOrderByEmissionDateDesc();
        } else {
            result = this.invoiceRepository.findByEmissionDateGreaterThanEqualOrderByEmissionDateDesc(criteria.getFromDate());
        }

        return result.stream()
                .map(InvoiceEntity::toDomain);
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return this.invoiceRepository.findById(id)
                .map(InvoiceEntity::toDomain);
    }

    @Override
    public Integer findNextNumber(String series) {
        return invoiceRepository.findFirstBySeriesOrderByNumberDesc(series)
                .map(InvoiceEntity::getNumber)
                .map(n -> n + 1)
                .orElse(FIRST_SERIES_NUMBER);
    }
}
