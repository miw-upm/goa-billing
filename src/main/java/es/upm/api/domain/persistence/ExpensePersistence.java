package es.upm.api.domain.persistence;

import es.upm.api.domain.model.Expense;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpensePersistence {
    void create(Expense expense);
}