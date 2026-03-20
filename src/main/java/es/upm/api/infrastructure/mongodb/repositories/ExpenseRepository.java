package es.upm.api.infrastructure.mongodb.repositories;

import es.upm.api.infrastructure.mongodb.entities.ExpenseEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface ExpenseRepository extends MongoRepository<ExpenseEntity, UUID> {
}