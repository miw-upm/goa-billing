package es.upm.api.adapter.in.resources;

import es.upm.api.domain.model.Payment;
import es.upm.api.domain.model.criteria.PaymentFindCriteria;
import es.upm.api.domain.services.PaymentService;
import es.upm.miw.security.Security;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.stream.Stream;

@PreAuthorize(Security.ADMIN_MANAGER_OPERATOR)
@RestController
@RequestMapping(PaymentResource.PAYMENTS)
@RequiredArgsConstructor
public class PaymentResource {
    public static final String PAYMENTS = "/payments";

    private final PaymentService paymentService;

    @PostMapping
    public Payment create(@Valid @RequestBody Payment request) {
        return this.paymentService.create(request);
    }

    @GetMapping("/{id}")
    public Payment read(@PathVariable UUID id) {
        return this.paymentService.read(id);
    }

    @PutMapping("/{id}")
    public Payment update(@PathVariable UUID id, @Valid @RequestBody Payment request) {
        return this.paymentService.update(id, request);
    }

    @PreAuthorize(Security.ADMIN)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        this.paymentService.delete(id);
    }

    @GetMapping
    public Stream<Payment> find(@ModelAttribute PaymentFindCriteria criteria) {
        return this.paymentService.find(criteria);
    }
}
