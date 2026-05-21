package es.upm.api.domain.model;

import es.upm.api.domain.model.criteria.ExpenseFindCriteria;
import es.upm.api.domain.model.criteria.IncomeFindCriteria;
import es.upm.api.domain.model.criteria.InvoiceFindCriteria;
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

        criteria.setDate(LocalDate.now());
        assertFalse(criteria.isEmpty());
    }

    @Test
    void shouldDetectEmptyIncomeFindCriteria() {
        IncomeFindCriteria criteria = new IncomeFindCriteria();

        assertTrue(criteria.isEmpty());

        criteria.setEngagementId(UUID.randomUUID());
        assertFalse(criteria.isEmpty());
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
