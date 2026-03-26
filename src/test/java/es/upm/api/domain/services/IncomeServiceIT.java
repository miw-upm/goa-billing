package es.upm.api.domain.services;

import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.persistence.IncomePersistence;
import es.upm.api.domain.webclients.EngagementWebClient;
import es.upm.api.domain.webclients.UserWebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class IncomeServiceIT {

    @Autowired
    private IncomeService incomeService;

    @MockitoBean
    private IncomePersistence incomePersistence;

    @MockitoBean
    private EngagementWebClient engagementWebClient;

    @MockitoBean
    private UserWebClient userWebClient;

    private Income income;

    @BeforeEach
    void setUp() {
        this.income = Income.builder()
                .engagementId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .amount(BigDecimal.valueOf(250))
                .date(LocalDate.of(2026, 3, 20))
                .build();
    }

    @Test
    void shouldCreateIncome() {
        when(this.engagementWebClient.readById(this.income.getEngagementId())).thenReturn(new Object());
        when(this.userWebClient.readUserById(this.income.getUserId())).thenReturn(null);

        Income createdIncome = this.incomeService.create(this.income);

        assertNotNull(createdIncome);
        assertNotNull(createdIncome.getId());
        assertEquals(this.income.getEngagementId(), createdIncome.getEngagementId());
        assertEquals(this.income.getUserId(), createdIncome.getUserId());
        assertEquals(this.income.getAmount(), createdIncome.getAmount());
        assertEquals(this.income.getDate(), createdIncome.getDate());

        ArgumentCaptor<Income> incomeCaptor = ArgumentCaptor.forClass(Income.class);
        verify(this.incomePersistence).create(incomeCaptor.capture());
        verify(this.engagementWebClient).readById(this.income.getEngagementId());
        verify(this.userWebClient).readUserById(this.income.getUserId());

        Income persistedIncome = incomeCaptor.getValue();
        assertNotNull(persistedIncome.getId());
        assertEquals(createdIncome.getId(), persistedIncome.getId());
        assertEquals(this.income.getEngagementId(), persistedIncome.getEngagementId());
        assertEquals(this.income.getUserId(), persistedIncome.getUserId());
        assertEquals(this.income.getAmount(), persistedIncome.getAmount());
        assertEquals(this.income.getDate(), persistedIncome.getDate());
    }

    @Test
    void shouldNotPersistIncomeWhenEngagementDoesNotExist() {
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.engagementWebClient.readById(this.income.getEngagementId())).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.incomeService.create(this.income));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.income.getEngagementId());
        verify(this.userWebClient, never()).readUserById(any());
        verify(this.incomePersistence, never()).create(any());
    }

    @Test
    void shouldNotPersistIncomeWhenUserDoesNotExist() {
        when(this.engagementWebClient.readById(this.income.getEngagementId())).thenReturn(new Object());
        RuntimeException exception = new RuntimeException("User not found");
        when(this.userWebClient.readUserById(this.income.getUserId())).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.incomeService.create(this.income));

        assertEquals("User not found", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.income.getEngagementId());
        verify(this.userWebClient).readUserById(this.income.getUserId());
        verify(this.incomePersistence, never()).create(any());
    }

    @Test
    void shouldNotPersistIncomeWhenDateIsFuture() {
        this.income.setDate(LocalDate.now().plusDays(1));

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.incomeService.create(this.income));

        assertEquals("Bad Request Exception. Income date cannot be in the future", thrown.getMessage());
        verifyNoInteractions(this.engagementWebClient);
        verifyNoInteractions(this.userWebClient);
        verify(this.incomePersistence, never()).create(any());
    }
}