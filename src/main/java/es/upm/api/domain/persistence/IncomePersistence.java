package es.upm.api.domain.persistence;

import es.upm.api.domain.model.Income;
import org.springframework.stereotype.Repository;

@Repository
public interface IncomePersistence {
    void create(Income income);
}