package es.upm.api.adapter.out.billing.mongo.invoice;

import es.upm.api.adapter.out.billing.mongo.expense.ExpenseEntity;
import es.upm.api.adapter.out.billing.mongo.payment.PaymentEntity;
import es.upm.api.domain.model.Invoice;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.ports.out.billing.InvoiceGateway;
import es.upm.miw.exception.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class InvoiceAdapter implements InvoiceGateway {
    private final InvoiceRepository invoiceRepository;

    public InvoiceAdapter(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public Invoice create(Invoice invoice) {
        return this.invoiceRepository.save(new InvoiceEntity(invoice)).toDomain();
    }

    @Override
    public Invoice update(UUID id, Invoice invoice) {
        InvoiceEntity invoiceEntity = this.invoiceRepository.findById(id.toString())
                .orElseThrow(() -> new NotFoundException("Invoice id: " + id));

        BeanUtils.copyProperties(invoice, invoiceEntity,
                "id", "billingInfo", "legalProcedures", "payments", "priorPayments", "expenses", "originalInvoice");
        invoiceEntity.setBillingInfo(invoice.getBillingInfo() == null ? null : new BillingInfoEntity(invoice.getBillingInfo()));
        invoiceEntity.setEngagementId(invoice.getEngagement() == null || invoice.getEngagement().getId() == null
                ? null : invoice.getEngagement().getId().toString());
        invoiceEntity.setLegalProcedures(invoice.getLegalProcedures() == null ? null
                : invoice.getLegalProcedures().stream().map(LegalProcedureEntity::new).toList());
        invoiceEntity.setPayments(invoice.getPayments() == null ? null
                : invoice.getPayments().stream().map(PaymentEntity::new).toList());
        invoiceEntity.setPriorPayments(invoice.getPriorPayments() == null ? null
                : invoice.getPriorPayments().stream().map(PaymentEntity::new).toList());
        invoiceEntity.setExpenses(invoice.getExpenses() == null ? null
                : invoice.getExpenses().stream().map(ExpenseEntity::new).toList());
        invoiceEntity.setOriginalInvoice(invoice.getOriginalInvoice() == null ? null
                : new OriginalInvoiceEntity(invoice.getOriginalInvoice()));

        return this.invoiceRepository.save(invoiceEntity).toDomain();
    }

    @Override
    public Invoice read(UUID id) {
        return this.invoiceRepository.findById(id.toString())
                .map(InvoiceEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Invoice id: " + id));
    }

    @Override
    public Invoice read(String series, Integer number) {
        return this.invoiceRepository.findBySeriesAndNumber(series, number)
                .map(InvoiceEntity::toDomain)
                .orElseThrow(() -> new NotFoundException("Invoice series: " + series + ", number: " + number));
    }

    @Override
    public void delete(UUID id) {
        InvoiceEntity invoiceEntity = this.invoiceRepository.findById(id.toString())
                .orElseThrow(() -> new NotFoundException("Invoice id: " + id));
        this.invoiceRepository.delete(invoiceEntity);
    }

    @Override
    public Stream<Invoice> find(InvoiceFindCriteria criteria) {
        List<InvoiceEntity> result;

        if (StringUtils.hasText(criteria.getEngagementId())) {
            String engagementIdPrefix = this.normalizeEngagementIdPrefix(criteria.getEngagementId());
            if (criteria.getFromDate() == null) {
                result = this.invoiceRepository.findByEngagementIdStartingWithOrderByEmissionDateDesc(engagementIdPrefix);
            } else {
                result = this.invoiceRepository.findByEngagementIdStartingWithAndEmissionDateGreaterThanEqualOrderByEmissionDateDesc(
                        engagementIdPrefix, criteria.getFromDate());
            }
        } else if (criteria.isEmpty() || criteria.getFromDate() == null) {
            result = this.invoiceRepository.findAllByOrderByEmissionDateDesc();
        } else {
            result = this.invoiceRepository.findByEmissionDateGreaterThanEqualOrderByEmissionDateDesc(criteria.getFromDate());
        }

        return result.stream()
                .map(InvoiceEntity::toDomain);
    }

    private String normalizeEngagementIdPrefix(String engagementId) {
        String normalized = engagementId.trim();
        return normalized.length() <= 4 ? normalized : normalized.substring(0, 4);
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return this.invoiceRepository.findById(id.toString())
                .map(InvoiceEntity::toDomain);
    }

    @Override
    public Optional<Integer> findLastNumber(String series) {
        return invoiceRepository.findFirstBySeriesOrderByNumberDesc(series)
                .map(InvoiceEntity::getNumber);
    }
}
