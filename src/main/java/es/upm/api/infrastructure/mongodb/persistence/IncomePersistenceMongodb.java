package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.model.Income;
import es.upm.api.domain.persistence.IncomePersistence;
import es.upm.api.infrastructure.mongodb.entities.IncomeEntity;
import es.upm.api.infrastructure.mongodb.repositories.IncomeRepository;
import org.springframework.stereotype.Repository;

@Repository
public class IncomePersistenceMongodb implements IncomePersistence {

    private final IncomeRepository incomeRepository;

    public IncomePersistenceMongodb(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    @Override
    public void create(Income income) {
        this.incomeRepository.save(new IncomeEntity(income));
    }
}