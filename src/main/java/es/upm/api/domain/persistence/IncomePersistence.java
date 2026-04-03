package es.upm.api.domain.persistence;

import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.IncomeFindCriteria;
import org.springframework.stereotype.Repository;


import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface IncomePersistence {
    void create(Income income);
    Income readById(UUID id);
    Stream<Income> findAll(IncomeFindCriteria criteria);
    Stream<Income> findByEngagementId(UUID engagementId);

    void update(UUID id, Income income);
}
