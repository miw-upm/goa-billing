package es.upm.api.infrastructure.mongodb.persistence;

import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.IncomeFindCriteria;
import es.upm.api.domain.persistence.IncomePersistence;
import es.upm.api.infrastructure.mongodb.entities.IncomeEntity;
import es.upm.api.infrastructure.mongodb.repositories.IncomeRepository;
import es.upm.miw.exception.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    public Income readById(UUID id) {
        return this.incomeRepository.findById(id)
                .map(IncomeEntity::toIncome)
                .orElseThrow(() -> new NotFoundException("Income id: " + id));
    }

    @Override
    public Stream<Income> findAll(IncomeFindCriteria criteria) {
        List<IncomeEntity> result;

        if (criteria.isEmpty()) {
            result = this.incomeRepository.findAll(DATE);
        } else if (criteria.getEngagementId() != null && criteria.getDate() != null) {
            result = this.incomeRepository.findByEngagementIdAndDate(
                    criteria.getEngagementId(),
                    criteria.getDate(),
                    DATE
            );
        } else if (criteria.getEngagementId() != null) {
            result = this.incomeRepository.findByEngagementId(criteria.getEngagementId(), DATE);
        } else {
            result = this.incomeRepository.findByDate(criteria.getDate(), DATE);
        }

        return result.stream()
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
                .orElseThrow(() -> new NotFoundException("Income id: " + id));
        // Only update allowed fields
        entity.setEngagementId(income.getEngagementId());
        entity.setUserId(income.getUserId());
        entity.setAmount(income.getAmount());
        entity.setDate(income.getDate());
        this.incomeRepository.save(entity);
    }
}
