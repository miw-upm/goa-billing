package es.upm.api.domain.services;

import es.upm.api.configurations.DatabaseSeederDev;
import es.upm.api.domain.ports.out.billing.PaymentGateway;
import es.upm.api.domain.ports.out.engagement.EngagementGateway;
import es.upm.api.domain.ports.out.user.UserFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest
@ActiveProfiles("test")
class InvoicePdfCheck {
    private static final Logger LOG = LogManager.getLogger(InvoicePdfCheck.class);

    @Autowired
    private InvoiceService invoiceService;

    @MockitoBean
    private PaymentGateway paymentGateway;

    @MockitoBean
    private EngagementGateway engagementGateway;

    @MockitoBean
    private UserFinder userFinder;

    @Test
    void testGenerateIssuedInvoicePdfCheck() throws Exception {
        byte[] pdf = this.invoiceService.generatePdf(DatabaseSeederDev.ID_14);
        Path output = Path.of("target", "invoice-issued-check.pdf");
        Files.write(output, pdf);
        LOG.info("PDF generado en: {}", output.toAbsolutePath());
    }

    @Test
    void testGenerateProformaInvoicePdfCheck() throws Exception {
        byte[] pdf = this.invoiceService.generatePdf(DatabaseSeederDev.ID_15);
        Path output = Path.of("target", "invoice-second-check.pdf");
        Files.write(output, pdf);
        LOG.info("PDF generado en: {}", output.toAbsolutePath());
    }
}
