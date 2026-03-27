package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.model.Income;
import es.upm.api.domain.persistence.IncomePersistence;
import es.upm.api.infrastructure.mongodb.entities.IncomeEntity;
import es.upm.api.infrastructure.mongodb.repositories.IncomeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.stream.Stream;

@Repository
public class IncomePersistenceMongodb implements IncomePersistence {
    public static final Sort DATE = Sort.by(Sort.Direction.DESC, "date");

    private final IncomeRepository incomeRepository;

    public IncomePersistenceMongodb(IncomeRepository incomeRepository) {
        this.incomeRepository = incomeRepository;
    }

    @Override
    public void create(Income income) {
        this.incomeRepository.save(new IncomeEntity(income));
    }

    @Override
    public Stream<Income> findAll() {
        return this.incomeRepository.findAll(DATE)
                .stream()
                .map(IncomeEntity::toIncome);
    }

    @Override
    public Stream<Income> findByEngagementId(UUID engagementId) {
        return this.incomeRepository.findByEngagementId(engagementId, DATE)
                .stream()
                .map(IncomeEntity::toIncome);
    }
    
    @Override
    public void update(java.util.UUID id, Income income) {
        IncomeEntity entity = this.incomeRepository.findById(id)
                .orElseThrow(() -> new es.upm.api.domain.exceptions.NotFoundException("Income not found: " + id));
        // Only update allowed fields
        entity.setEngagementId(income.getEngagementId());
        entity.setUserId(income.getUserId());
        entity.setAmount(income.getAmount());
        entity.setDate(income.getDate());
        this.incomeRepository.save(entity);
    }
}