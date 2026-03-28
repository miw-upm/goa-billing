package es.upm.api.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseFindCriteriaTest {

    @Test
    void testAllWhenDateIsNull() {
        ExpenseFindCriteria criteria = new ExpenseFindCriteria();
        assertTrue(criteria.all());
    }

    @Test
    void testAllWhenDateIsNotNull() {
        ExpenseFindCriteria criteria = new ExpenseFindCriteria(LocalDate.now());
        assertFalse(criteria.all());
    }

    @Test
    void testGettersAndSetters() {
        ExpenseFindCriteria criteria = new ExpenseFindCriteria();
        LocalDate date = LocalDate.of(2026, 1, 1);
        criteria.setDate(date);
        assertEquals(date, criteria.getDate());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDate date = LocalDate.of(2026, 1, 1);
        ExpenseFindCriteria criteria = new ExpenseFindCriteria(date);
        assertEquals(date, criteria.getDate());
    }

    @Test
    void testNoArgsConstructor() {
        ExpenseFindCriteria criteria = new ExpenseFindCriteria();
        assertNull(criteria.getDate());
    }
}

