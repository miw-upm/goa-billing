package es.upm.api.domain.services;

import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.model.Income;
import es.upm.api.domain.persistence.IncomePersistence;
import es.upm.api.domain.webclients.EngagementWebClient;
import es.upm.api.domain.webclients.UserWebClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class IncomeService {

    private final IncomePersistence incomePersistence;
    private final EngagementWebClient engagementWebClient;
    private final UserWebClient userWebClient;

    public IncomeService(IncomePersistence incomePersistence,
                         EngagementWebClient engagementWebClient,
                         UserWebClient userWebClient) {
        this.incomePersistence = incomePersistence;
        this.engagementWebClient = engagementWebClient;
        this.userWebClient = userWebClient;
    }

    public Income create(Income income) {
        if (income.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Income date cannot be in the future");
        }
        income.setId(UUID.randomUUID());
        this.engagementWebClient.readById(income.getEngagementId());
        this.userWebClient.readUserById(income.getUserId());
        this.incomePersistence.create(income);
        return income;
    }

    public Stream<Income> findAll(UUID engagementId) {
        if (engagementId == null) {
            return this.incomePersistence.findAll();
        }
        this.engagementWebClient.readById(engagementId);
        return this.incomePersistence.findByEngagementId(engagementId);
    }

    public Income update(UUID id, Income income) {
        if (income.getDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Income date cannot be in the future");
        }
        this.engagementWebClient.readById(income.getEngagementId());
        this.userWebClient.readUserById(income.getUserId());
        // Ensure the id is not changed
        income.setId(id);
        this.incomePersistence.update(id, income);
        return income;
    }
}