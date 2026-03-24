package es.upm.api.domain.services;

import es.upm.api.domain.model.Income;
import es.upm.api.domain.persistence.IncomePersistence;
import es.upm.api.domain.webclients.EngagementWebClient;
import es.upm.api.domain.webclients.UserWebClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

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
            throw new IllegalArgumentException("Income date cannot be in the future");
        }

        income.setId(UUID.randomUUID());
        this.engagementWebClient.readById(income.getEngagementId());
        this.userWebClient.readUserById(income.getUserId());
        this.incomePersistence.create(income);
        return income;
    }
}