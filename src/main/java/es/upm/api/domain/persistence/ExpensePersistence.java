package es.upm.api.domain.persistence;

import es.upm.api.domain.model.Expense;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExpensePersistence {
    void create(Expense expense);

    Expense readById(UUID id);
}
