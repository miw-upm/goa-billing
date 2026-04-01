package es.upm.api.infrastructure.mongodb.repositories;

import es.upm.api.infrastructure.mongodb.entities.InvoiceEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceRepository extends MongoRepository<InvoiceEntity, UUID> {
    List<InvoiceEntity> findByEngagementId(UUID engagementId);
    InvoiceEntity findByExpensesId(UUID expenseId);
    InvoiceEntity findByIncomesId(UUID incomeId);
}
