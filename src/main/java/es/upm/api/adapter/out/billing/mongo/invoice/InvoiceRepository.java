package es.upm.api.adapter.out.billing.mongo.invoice;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends MongoRepository<InvoiceEntity, UUID> {
    List<InvoiceEntity> findByEngagementId(UUID engagementId, Sort sort);

    List<InvoiceEntity> findByDate(LocalDate date, Sort sort);

    List<InvoiceEntity> findByEngagementIdAndDate(UUID engagementId, LocalDate date, Sort sort);

    InvoiceEntity findByExpensesId(UUID expenseId);

    InvoiceEntity findByIncomesId(UUID incomeId);
}
