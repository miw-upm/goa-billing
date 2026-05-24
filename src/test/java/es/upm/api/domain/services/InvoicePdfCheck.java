package es.upm.api.domain.services;

import es.upm.api.configurations.DatabaseSeederDev;
import es.upm.api.domain.ports.out.billing.PaymentGateway;
import es.upm.api.domain.ports.out.engagement.EngagementFinder;
import es.upm.api.domain.ports.out.user.UserFinder;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Files;
import java.nio.file.Path;

@Log4j2
@SpringBootTest
@ActiveProfiles("test")
class InvoicePdfCheck {

    @Autowired
    private InvoiceService invoiceService;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @MockitoBean
    private EngagementFinder engagementFinder;

    @MockitoBean
    private UserFinder userFinder;

    @Test
    void testGenerateIssuedInvoicePdfCheck() throws Exception {
        byte[] pdf = this.invoiceService.generatePdf(DatabaseSeederDev.ID_14);
        Path output = Path.of("target", "invoice-issued-check.pdf");
        Files.write(output, pdf);
        log.info("PDF generado en: {}", output.toAbsolutePath());
    }

    @Test
    void testGenerateProformaInvoicePdfCheck() throws Exception {
        byte[] pdf = this.invoiceService.generatePdf(DatabaseSeederDev.ID_15);
        Path output = Path.of("target", "invoice-second-check.pdf");
        Files.write(output, pdf);
        log.info("PDF generado en: {}", output.toAbsolutePath());
    }
}
