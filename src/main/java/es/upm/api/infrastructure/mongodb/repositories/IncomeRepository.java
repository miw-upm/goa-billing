package es.upm.api.infrastructure.mongodb.repositories;

import es.upm.api.infrastructure.mongodb.entities.IncomeEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface IncomeRepository extends MongoRepository<IncomeEntity, UUID> {
}