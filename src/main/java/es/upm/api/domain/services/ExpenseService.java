package es.upm.api.domain.services;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.ports.out.billing.ExpenseGateway;
import es.upm.api.domain.ports.out.engagement.EngagementFinder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseGateway expenseGateway;
    private final EngagementFinder engagementFinder;

    public void create(Expense expense) {
        expense.setId(UUID.randomUUID());
        expense.setRecordedAt(LocalDateTime.now());
        if (expense.getEngagement() != null){
            expense.setEngagement(this.engagementFinder.read(expense.getEngagement().getId()));
        }
        expense.setDocumentPath(null); //TODO
        this.expenseGateway.create(expense);
    }

    public Expense read(UUID id) {
        Expense expense = this.expenseGateway.read(id);
        if (expense.getEngagement() != null){
            expense.setEngagement(this.engagementFinder.read(expense.getEngagement().getId()));
        }
        return expense;
    }

    public void update(UUID id, Expense expense) {
        Expense currentExpense = this.expenseGateway.read(id);
        expense.setId(id);
        expense.setRecordedAt(LocalDateTime.now());
        expense.setDocumentPath(currentExpense.getDocumentPath());
        if (expense.getEngagement() != null){
            expense.setEngagement(this.engagementFinder.read(expense.getEngagement().getId()));
        }
        this.expenseGateway.update(id, expense);
    }

    public void delete(UUID id) {
        this.expenseGateway.delete(id);
    }

    public Stream<Expense> find(ExpenseFindCriteria criteria) {
        return this.expenseGateway.find(criteria);
    }

    public Stream<SupplierInfo> findSuppliers(String supplier) {
        return this.expenseGateway.findSuppliers(supplier);
    }
}
