package es.upm.api.domain.ports.out.billing;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface ExpenseGateway {
    void create(Expense expense);

    Expense update(UUID id, Expense expense);

    Expense readById(UUID id);

    Stream<Expense> findAll(ExpenseFindCriteria criteria);
}
