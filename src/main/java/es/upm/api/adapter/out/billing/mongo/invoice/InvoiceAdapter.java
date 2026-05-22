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

    public static final Sort DATE = Sort.by(Sort.Direction.DESC, "emissionDate");

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
        invoiceEntity.setEngagementId(invoice.getEngagement().getEngagementId());
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

        if (criteria.isEmpty()) {
            result = this.invoiceRepository.findAll(DATE);
        } else if (criteria.getEngagementId() != null) {
            result = this.invoiceRepository.findByEngagementId(criteria.getEngagementId());
            if (criteria.getDate() != null) {
                result = result.stream()
                        .filter(invoiceEntity -> invoiceEntity.getEmissionDate().equals(criteria.getDate()))
                        .toList();
            }
        } else {
            result = this.invoiceRepository.findByEmissionDate(criteria.getDate());
        }

        return result.stream()
                .map(InvoiceEntity::toDomain);
    }
}
