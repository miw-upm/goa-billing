package es.upm.api.domain.services;

import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.model.IncomeFindCriteria;
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
    private final IncomeFindCriteria criteria = new IncomeFindCriteria();

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
    void shouldReadIncomeById() {
        this.income.setId(UUID.randomUUID());
        when(this.incomePersistence.readById(this.income.getId())).thenReturn(this.income);

        Income result = this.incomeService.readById(this.income.getId());

        assertEquals(this.income, result);
        verify(this.incomePersistence).readById(this.income.getId());
        verifyNoInteractions(this.engagementWebClient);
        verifyNoInteractions(this.userWebClient);
    }

    @Test
    void shouldThrowNotFoundWhenIncomeDoesNotExist() {
        UUID id = UUID.randomUUID();
        String expectedDetail = "Income id: " + id;
        String expectedMessage = "Not Found Exception. " + expectedDetail;
        when(this.incomePersistence.readById(id)).thenThrow(new es.upm.api.domain.exceptions.NotFoundException(expectedDetail));

        es.upm.api.domain.exceptions.NotFoundException thrown = assertThrows(es.upm.api.domain.exceptions.NotFoundException.class,
                () -> this.incomeService.readById(id));

        assertEquals(expectedMessage, thrown.getMessage());
        verify(this.incomePersistence).readById(id);
        verifyNoInteractions(this.engagementWebClient);
        verifyNoInteractions(this.userWebClient);
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
        Income updateData = Income.builder()
                .engagementId(this.income.getEngagementId())
                .userId(this.income.getUserId())
                .amount(BigDecimal.valueOf(500))
                .date(LocalDate.of(2026, 3, 25))
                .build();

        when(this.engagementWebClient.readById(updateData.getEngagementId())).thenReturn(new Object());
        when(this.userWebClient.readUserById(updateData.getUserId()))
                .thenReturn(es.upm.api.domain.model.UserDto.builder().id(updateData.getUserId()).build());
        doNothing().when(this.incomePersistence).update(id, updateData);

        Income response = this.incomeService.update(id, updateData);

        assertEquals(updateData.getEngagementId(), response.getEngagementId());
        assertEquals(updateData.getUserId(), response.getUserId());
        assertEquals(updateData.getAmount(), response.getAmount());
        assertEquals(updateData.getDate(), response.getDate());
        assertEquals(id, response.getId());
        verify(this.engagementWebClient).readById(updateData.getEngagementId());
        verify(this.userWebClient).readUserById(updateData.getUserId());
        verify(this.incomePersistence).update(id, updateData);
    }

    @Test
    void shouldNotUpdateIncomeWhenEngagementDoesNotExist() {
        UUID id = UUID.randomUUID();
        Income updateData = Income.builder()
                .engagementId(this.income.getEngagementId())
                .userId(this.income.getUserId())
                .amount(BigDecimal.valueOf(500))
                .date(LocalDate.of(2026, 3, 25))
                .build();
        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.engagementWebClient.readById(updateData.getEngagementId())).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.incomeService.update(id, updateData));

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.engagementWebClient).readById(updateData.getEngagementId());
        verify(this.userWebClient, never()).readUserById(any());
        verify(this.incomePersistence, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateIncomeWhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();
        Income updateData = Income.builder()
                .engagementId(this.income.getEngagementId())
                .userId(this.income.getUserId())
                .amount(BigDecimal.valueOf(500))
                .date(LocalDate.of(2026, 3, 25))
                .build();
        when(this.engagementWebClient.readById(updateData.getEngagementId())).thenReturn(new Object());
        RuntimeException exception = new RuntimeException("User not found");
        when(this.userWebClient.readUserById(updateData.getUserId())).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.incomeService.update(id, updateData));

        assertEquals("User not found", thrown.getMessage());
        verify(this.engagementWebClient).readById(updateData.getEngagementId());
        verify(this.userWebClient).readUserById(updateData.getUserId());
        verify(this.incomePersistence, never()).update(any(), any());
    }

    @Test
    void shouldNotUpdateIncomeWhenDateIsFuture() {
        UUID id = UUID.randomUUID();
        Income updateData = Income.builder()
                .engagementId(this.income.getEngagementId())
                .userId(this.income.getUserId())
                .amount(BigDecimal.valueOf(500))
                .date(LocalDate.now().plusDays(1))
                .build();

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> this.incomeService.update(id, updateData));

        assertEquals("Bad Request Exception. Income date cannot be in the future", thrown.getMessage());
        verifyNoInteractions(this.engagementWebClient);
        verifyNoInteractions(this.userWebClient);
        verify(this.incomePersistence, never()).update(any(), any());
    }

    @Test
    void shouldFindAllIncomes() {
        Income incomeA = buildIncome(UUID.randomUUID(), this.income.getEngagementId(), this.income.getUserId(),
                BigDecimal.valueOf(120), LocalDate.of(2026, 3, 21));
        Income incomeB = buildIncome(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(90), this.income.getDate());

        when(this.incomePersistence.findAll(criteria)).thenReturn(Stream.of(incomeA, incomeB));

        List<Income> incomes = this.incomeService.findAll(criteria).toList();

        assertEquals(2, incomes.size());
        assertEquals(List.of(incomeA, incomeB), incomes);
        verify(this.incomePersistence).findAll(criteria);
        verifyNoInteractions(this.engagementWebClient);
    }

    @Test
    void shouldReturnEmptyWhenNoIncomesExist() {
        when(this.incomePersistence.findAll(this.criteria)).thenReturn(Stream.empty());

        List<Income> incomes = this.incomeService.findAll(this.criteria).toList();

        assertTrue(incomes.isEmpty());
        verify(this.incomePersistence).findAll(this.criteria);
        verifyNoInteractions(this.engagementWebClient);
    }

    @Test
    void shouldFindIncomesByEngagementId() {
        Income incomeA = buildIncome(UUID.randomUUID(), this.income.getEngagementId(), UUID.randomUUID(),
                BigDecimal.valueOf(120), LocalDate.of(2026, 3, 21));
        Income incomeB = buildIncome(UUID.randomUUID(), this.income.getEngagementId(), UUID.randomUUID(),
                BigDecimal.valueOf(90), this.income.getDate());

        IncomeFindCriteria criteria = new IncomeFindCriteria(this.income.getEngagementId(), null);

        when(this.engagementWebClient.readById(this.income.getEngagementId())).thenReturn(new Object());
        when(this.incomePersistence.findAll(criteria)).thenReturn(Stream.of(incomeA, incomeB));

        List<Income> incomes = this.incomeService.findAll(criteria).toList();

        assertEquals(2, incomes.size());
        assertEquals(List.of(incomeA, incomeB), incomes);
        verify(this.engagementWebClient).readById(this.income.getEngagementId());
        verify(this.incomePersistence).findAll(criteria);
    }

    @Test
    void shouldFailFindIncomesByInvalidEngagementId() {
        IncomeFindCriteria criteria = new IncomeFindCriteria(this.income.getEngagementId(), null);

        RuntimeException exception = new RuntimeException("Engagement not found");
        when(this.engagementWebClient.readById(this.income.getEngagementId())).thenThrow(exception);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> this.incomeService.findAll(criteria).toList());

        assertEquals("Engagement not found", thrown.getMessage());
        verify(this.engagementWebClient).readById(this.income.getEngagementId());
        verify(this.incomePersistence, never()).findAll(any());
    }

    @Test
    void shouldFindIncomesByDate() {
        Income incomeA = buildIncome(UUID.randomUUID(), this.income.getEngagementId(), this.income.getUserId(),
                BigDecimal.valueOf(120), this.income.getDate());
        Income incomeB = buildIncome(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.valueOf(90), this.income.getDate());

        IncomeFindCriteria criteria = new IncomeFindCriteria(null, this.income.getDate());

        when(this.incomePersistence.findAll(criteria)).thenReturn(Stream.of(incomeA, incomeB));

        List<Income> incomes = this.incomeService.findAll(criteria).toList();

        assertEquals(2, incomes.size());
        assertEquals(List.of(incomeA, incomeB), incomes);
        verify(this.incomePersistence).findAll(criteria);
        verifyNoInteractions(this.engagementWebClient);
    }
}
