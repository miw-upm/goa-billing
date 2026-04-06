package es.upm.api.domain.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainExceptionsTest {

    @Test
    void shouldBuildForbiddenExceptionMessage() {
        ForbiddenException exception = new ForbiddenException("role not allowed");

        assertEquals("Forbidden Exception. role not allowed", exception.getMessage());
    }

    @Test
    void shouldBuildConflictExceptionMessage() {
        ConflictException exception = new ConflictException("already exists");

        assertEquals("Conflict Exception. already exists", exception.getMessage());
    }

    @Test
    void shouldBuildBadGatewayExceptionMessage() {
        BadGatewayException exception = new BadGatewayException("remote timeout");

        assertEquals("Bad Gateway Exception. remote timeout", exception.getMessage());
    }
}
