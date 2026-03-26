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
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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

    // Helper para crear Income
    private Income buildIncome(UUID id, UUID engagementId, UUID userId, BigDecimal amount, LocalDate date) {
        return Income.builder()
                .id(id)
                .engagementId(engagementId)
                .userId(userId)
                .amount(amount)
                .date(date)
                .build();
    }

    @Test
    void shouldUpdateIncome() {
        UUID id = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Income updateData = buildIncome(null, engagementId, userId, BigDecimal.valueOf(500), LocalDate.of(2026, 3, 25));
        Income updatedIncome = buildIncome(id, engagementId, userId, BigDecimal.valueOf(500), LocalDate.of(2026, 3, 25));

        when(this.engagementWebClient.readById(engagementId)).thenReturn(new Object());
        when(this.userWebClient.readUserById(userId)).thenReturn(es.upm.api.domain.model.UserDto.builder().id(userId).build());
        // Simula persistencia
        doNothing().when(this.incomePersistence).update(id, updateData);

        Income response = this.incomeService.update(id, updateData);

        assertEquals(updatedIncome.getEngagementId(), response.getEngagementId());
        assertEquals(updatedIncome.getUserId(), response.getUserId());
        assertEquals(updatedIncome.getAmount(), response.getAmount());
        assertEquals(updatedIncome.getDate(), response.getDate());
        assertEquals(id, response.getId());
        verify(this.engagementWebClient).readById(engagementId);
        verify(this.userWebClient).readUserById(userId);
        verify(this.incomePersistence).update(id, updateData);
    }

    @Test
    void shouldNotUpdateIncomeWhenEngagementDoesNotExist() {
        UUID id = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        Income updateData = buildIncome(null, engagementId, UUID.randomUUID(), BigDecimal.valueOf(500), LocalDate.of(2026, 3, 25));
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.engagementWebClient.readById(engagementId)).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.incomeService.update(id, updateData));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.engagementWebClient).readById(engagementId);
        verify(this.userWebClient, never()).readUserById(any());
        verify(this.incomePersistence, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateIncomeWhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Income updateData = buildIncome(null, engagementId, userId, BigDecimal.valueOf(500), LocalDate.of(2026, 3, 25));
        when(this.engagementWebClient.readById(engagementId)).thenReturn(new Object());
        RuntimeException exception = new RuntimeException("User not found");
        when(this.userWebClient.readUserById(userId)).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.incomeService.update(id, updateData));

        assertEquals("User not found", thrown.getMessage());
        verify(this.engagementWebClient).readById(engagementId);
        verify(this.userWebClient).readUserById(userId);
        verify(this.incomePersistence, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateIncomeWhenDateIsFuture() {
        UUID id = UUID.randomUUID();
        UUID engagementId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Income updateData = buildIncome(null, engagementId, userId, BigDecimal.valueOf(500), LocalDate.now().plusDays(1));

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.incomeService.update(id, updateData));

        assertEquals("Bad Request Exception. Income date cannot be in the future", thrown.getMessage());
        verifyNoInteractions(this.engagementWebClient);
        verifyNoInteractions(this.userWebClient);
        verify(this.incomePersistence, never()).update(any(), any());
    }

    @Test
    void shouldFindAllIncomes() {
        Income incomeA = buildIncome(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(120), LocalDate.of(2026, 3, 21));
        Income incomeB = buildIncome(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(90), LocalDate.of(2026, 3, 20));

        when(this.incomePersistence.findAll()).thenReturn(Stream.of(incomeA, incomeB));

        List<Income> incomes = this.incomeService.findAll().toList();

        assertEquals(2, incomes.size());
        assertEquals(List.of(incomeA, incomeB), incomes);
        verify(this.incomePersistence).findAll();
    }

    @Test
    void shouldReturnEmptyWhenNoIncomesExist() {
        when(this.incomePersistence.findAll()).thenReturn(Stream.empty());

        List<Income> incomes = this.incomeService.findAll().toList();

        assertTrue(incomes.isEmpty());
        verify(this.incomePersistence).findAll();
    }

}