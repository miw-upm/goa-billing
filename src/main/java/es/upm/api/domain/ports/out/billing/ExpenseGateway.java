package es.upm.api.domain.ports.out.billing;

import es.upm.api.domain.model.Expense;
import es.upm.api.domain.model.SupplierInfo;
import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
public interface ExpenseGateway {
    void create(Expense expense);

    Expense update(UUID id, Expense expense);

    Expense read(UUID id);

    Stream<Expense> find(ExpenseFindCriteria criteria);

    Stream<Expense> findByEngagementId(UUID engagementId);

    Stream<SupplierInfo> findSuppliers(String supplier);

    Integer findNextNumber(String series, Integer depreciationRate);

    Stream<Expense> findInvoiceReceivedBook(LocalDate fromDate, LocalDate toDate, BigDecimal taxableBaseThreshold);

    Stream<Expense> findInvoiceReceivedBook(String series, int fromNumber, int toNumber, BigDecimal taxableBaseThreshold);

    Stream<Expense> findCurrentExpensesBook(LocalDate fromDate, LocalDate toDate);

    Stream<Expense> findInvoiceReceivedInvestmentBook(LocalDate fromDate, LocalDate toDate, BigDecimal taxableBaseThreshold);

    Stream<Expense> findInvestmentAssetsUntil(LocalDate toDate);

    long countInvoiceReceivedBook(LocalDate fromDate, LocalDate toDate, BigDecimal taxableBaseThreshold);
}
