package es.upm.api.domain.model;

import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.model.criteria.PaymentFindCriteria;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindCriteriaTest {

    @Test
    void shouldDetectEmptyExpenseFindCriteria() {
        ExpenseFindCriteria criteria = new ExpenseFindCriteria();

        assertTrue(criteria.isEmpty());

        criteria.setFromDate(LocalDate.now());
        assertFalse(criteria.isEmpty());

        criteria = new ExpenseFindCriteria();
        criteria.setCategory("OTROS");
        assertFalse(criteria.isEmpty());

        criteria = new ExpenseFindCriteria();
        criteria.setSupplier("Taxi");
        assertFalse(criteria.isEmpty());

        criteria = new ExpenseFindCriteria();
        criteria.setEngagementId("2H60");
        assertFalse(criteria.isEmpty());
    }

    @Test
    void shouldDetectEmptyPaymentFindCriteria() {
        PaymentFindCriteria criteria = new PaymentFindCriteria();

        assertTrue(criteria.all());

        criteria.setClient("client");
        assertFalse(criteria.all());

        criteria = new PaymentFindCriteria();
        criteria.setEngagementId("2H60");
        assertFalse(criteria.all());
    }

    @Test
    void shouldDetectEmptyInvoiceFindCriteria() {
        InvoiceFindCriteria criteria = new InvoiceFindCriteria();

        assertTrue(criteria.isEmpty());

        criteria.setClient("john");
        assertFalse(criteria.isEmpty());

        criteria = new InvoiceFindCriteria();
        criteria.setFromDate(LocalDate.now());
        assertFalse(criteria.isEmpty());

        criteria = new InvoiceFindCriteria();
        criteria.setEngagementId("2H60");
        assertFalse(criteria.isEmpty());

        criteria = new InvoiceFindCriteria();
        criteria.setIssued(true);
        assertFalse(criteria.isEmpty());
    }
}
