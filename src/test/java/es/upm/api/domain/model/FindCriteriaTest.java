package es.upm.api.domain.model;

import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
import es.upm.api.domain.model.criteria.PaymentFindCriteria;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FindCriteriaTest {

    @Test
    void shouldDetectEmptyExpenseFindCriteria() {
        ExpenseFindCriteria criteria = new ExpenseFindCriteria();

        assertTrue(criteria.isEmpty());

        criteria.setFromDate(LocalDate.now());
        assertFalse(criteria.isEmpty());
    }

    @Test
    void shouldDetectEmptyPaymentFindCriteria() {
        PaymentFindCriteria criteria = new PaymentFindCriteria();

        assertTrue(criteria.all());

        criteria.setClient("client");
        assertFalse(criteria.all());
    }

    @Test
    void shouldDetectEmptyInvoiceFindCriteria() {
        InvoiceFindCriteria criteria = new InvoiceFindCriteria();

        assertTrue(criteria.isEmpty());

        criteria.setEngagementId(UUID.randomUUID());
        criteria.setDate(LocalDate.now());
        assertFalse(criteria.isEmpty());
    }
}
