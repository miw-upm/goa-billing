package es.upm.api.infrastructure.mongodb.repositories;

import es.upm.api.infrastructure.mongodb.entities.IncomeEntity;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface IncomeRepository extends MongoRepository<IncomeEntity, UUID> {
    List<IncomeEntity> findByEngagementId(UUID engagementId, Sort sort);
}