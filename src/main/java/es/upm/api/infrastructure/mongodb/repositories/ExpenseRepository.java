package es.upm.api.infrastructure.mongodb.repositories;

import es.upm.api.infrastructure.mongodb.entities.ExpenseEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends MongoRepository<ExpenseEntity, UUID> {
    @Query("{'date': ?0}")
    List<ExpenseEntity> findByDate(LocalDate date);

    @Query("{'engagementId': ?0}")
    List<ExpenseEntity> findByEngagementId(UUID engagementId);
}