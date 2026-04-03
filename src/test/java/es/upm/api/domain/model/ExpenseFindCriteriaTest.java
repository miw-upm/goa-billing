package es.upm.api.domain.model;

import es.upm.api.domain.exceptions.BadGatewayException;
import es.upm.api.domain.exceptions.BadRequestException;
import es.upm.api.domain.exceptions.ConflictException;
import es.upm.api.domain.exceptions.ForbiddenException;
import es.upm.api.domain.exceptions.NotFoundException;
import es.upm.api.domain.model.validations.AtLeastOneInvoiceItemValidator;
import es.upm.api.domain.model.validations.ListNotEmptyValidator;
import es.upm.api.domain.model.validations.PositiveBigDecimalValidator;
import es.upm.api.domain.services.UUIDBase64;
import es.upm.api.infrastructure.resources.httperrors.ApiExceptionHandler;
import es.upm.api.infrastructure.resources.httperrors.ErrorMessage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseFindCriteriaTest {
    private final UUID ENGAGEMENT_UUID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeee0000");

    private final ListNotEmptyValidator listNotEmptyValidator = new ListNotEmptyValidator();
    private final PositiveBigDecimalValidator positiveBigDecimalValidator = new PositiveBigDecimalValidator();
    private final AtLeastOneInvoiceItemValidator atLeastOneInvoiceItemValidator = new AtLeastOneInvoiceItemValidator();

    @Test
    void testAllWhenDateIsNull() {
        ExpenseFindCriteria criteria = new ExpenseFindCriteria();
        assertTrue(criteria.all());
    }

    @Test
    void testAllWhenDateAndEngagementIdIsNotNull() {
        ExpenseFindCriteria criteria = new ExpenseFindCriteria(ENGAGEMENT_UUID, LocalDate.now());
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
        ExpenseFindCriteria criteria = new ExpenseFindCriteria(ENGAGEMENT_UUID, date);
        assertEquals(date, criteria.getDate());
    }

    @Test
    void testNoArgsConstructor() {
        ExpenseFindCriteria criteria = new ExpenseFindCriteria();
        assertNull(criteria.getDate());
    }

    @Test
    void shouldValidateListNotEmptyConstraint() {
        listNotEmptyValidator.initialize(null);

        assertFalse(listNotEmptyValidator.isValid(null, null));
        assertFalse(listNotEmptyValidator.isValid(Collections.emptyList(), null));
        assertTrue(listNotEmptyValidator.isValid(List.of("income"), null));
    }

    @Test
    void shouldValidatePositiveBigDecimalConstraint() {
        positiveBigDecimalValidator.initialize(null);

        assertFalse(positiveBigDecimalValidator.isValid(null, null));
        assertFalse(positiveBigDecimalValidator.isValid(new BigDecimal("-0.01"), null));
        assertTrue(positiveBigDecimalValidator.isValid(BigDecimal.ZERO, null));
        assertTrue(positiveBigDecimalValidator.isValid(new BigDecimal("10.50"), null));
    }

    @Test
    void shouldRequireAtLeastOneInvoiceItem() {
        assertFalse(atLeastOneInvoiceItemValidator.isValid(null, null));
        assertFalse(atLeastOneInvoiceItemValidator.isValid(new InvoiceItemsHolder(List.of(), null), null));
        assertTrue(
                atLeastOneInvoiceItemValidator.isValid(new InvoiceItemsHolder(List.of("expense-id"), List.of()), null));
        assertTrue(atLeastOneInvoiceItemValidator.isValid(new InvoiceItemIdsHolder(List.of(), List.of("income-id")),
                null));
    }

    @Test
    void shouldBuildExpectedDomainExceptionMessages() {
        assertAll(
                () -> assertException(new BadGatewayException("gateway down"), "Bad Gateway Exception. gateway down"),
                () -> assertException(new BadRequestException("bad request"), "Bad Request Exception. bad request"),
                () -> assertException(new ConflictException("duplicated"), "Conflict Exception. duplicated"),
                () -> assertException(new ForbiddenException("no permissions"), "Forbidden Exception. no permissions"),
                () -> assertException(new NotFoundException("missing"), "Not Found Exception. missing"));
    }

    @Test
    void shouldEncodeAndDecodeUuidBase64Values() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        for (UUIDBase64 uuidBase64 : UUIDBase64.values()) {
            String encoded = uuidBase64.encode();
            assertEquals(22, encoded.length());
            assertFalse(encoded.contains("="));
            assertEquals(16, Base64.getUrlDecoder().decode(toUrlSafe(encoded)).length);
        }

        assertEquals(uuid, UUIDBase64.BASIC.decode(encode(uuid, Base64.getEncoder())));
        assertEquals(uuid, UUIDBase64.MIME.decode(encode(uuid, Base64.getMimeEncoder())));
        assertEquals(uuid, UUIDBase64.URL.decode(encode(uuid, Base64.getUrlEncoder())));
    }

    @Test
    void shouldMapExceptionsToErrorMessages() {
        ApiExceptionHandler handler = new ApiExceptionHandler(new MockEnvironment());

        assertDoesNotThrow(() -> handler.unauthorizedRequest(new AccessDeniedException("denied")));

        ErrorMessage noResource = handler.noResourceFoundRequest(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertEquals("NotFoundException", noResource.getError());
        assertEquals(HttpStatus.NOT_FOUND.value(), noResource.getCode());

        assertErrorMessage(handler.notFoundRequest(new NotFoundException("income")), "NotFoundException",
                "Not Found Exception. income", 404);
        assertErrorMessage(handler.badRequest(new BadRequestException("request")), "BadRequestException",
                "Bad Request Exception. request", 400);
        assertErrorMessage(handler.conflict(new ConflictException("conflict")), "ConflictException",
                "Conflict Exception. conflict", 409);
        assertErrorMessage(handler.forbidden(new ForbiddenException("forbidden")), "ForbiddenException",
                "Forbidden Exception. forbidden", 403);
        assertErrorMessage(handler.badGateway(new BadGatewayException("gateway")), "BadGatewayException",
                "Bad Gateway Exception. gateway", 502);
    }

    @Test
    void shouldExposeUnexpectedExceptionsInTestProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        ApiExceptionHandler handler = new ApiExceptionHandler(environment);

        ErrorMessage errorMessage = handler.exception(new IllegalStateException("boom"));

        assertEquals("IllegalStateException", errorMessage.getError());
        assertEquals("boom", errorMessage.getMessage());
        assertEquals(500, errorMessage.getCode());
        assertEquals("ErrorMessage{error='IllegalStateException', message='boom', code=500}", errorMessage.toString());
    }

    private static void assertException(RuntimeException exception, String expectedMessage) {
        assertInstanceOf(RuntimeException.class, exception);
        assertEquals(expectedMessage, exception.getMessage());
    }

    private static void assertErrorMessage(ErrorMessage errorMessage, String error, String message, int code) {
        assertEquals(error, errorMessage.getError());
        assertEquals(message, errorMessage.getMessage());
        assertEquals(code, errorMessage.getCode());
    }

    private static String encode(UUID uuid, Base64.Encoder encoder) {
        ByteBuffer buffer = ByteBuffer.allocate(16)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits());
        return encoder.withoutPadding().encodeToString(buffer.array());
    }

    private static String toUrlSafe(String encoded) {
        return encoded.replace('+', '-').replace('/', '_');
    }


    private static final class InvoiceItemIdsHolder {
        private final Collection<String> expenseIds;
        private final Collection<String> incomeIds;

        private InvoiceItemIdsHolder(Collection<String> expenseIds, Collection<String> incomeIds) {
            this.expenseIds = expenseIds;
            this.incomeIds = incomeIds;
        }

        public Collection<String> getExpenseIds() {
            return expenseIds;
        }

        public Collection<String> getIncomeIds() {
            return incomeIds;
        }
    }

    private static final class InvoiceItemsHolder {
        private final Collection<String> expenses;
        private final Collection<String> incomes;

        private InvoiceItemsHolder(Collection<String> expenses, Collection<String> incomes) {
            this.expenses = expenses;
            this.incomes = incomes;
        }

        public Collection<String> getExpenses() {
            return expenses;
        }

        public Collection<String> getIncomes() {
            return incomes;
        }
    }
}
