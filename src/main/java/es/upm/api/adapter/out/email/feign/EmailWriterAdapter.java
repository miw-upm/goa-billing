package es.upm.api.adapter.out.email.feign;

import es.upm.api.domain.ports.out.email.EmailWriter;
import es.upm.miw.exception.BadGatewayException;
import es.upm.miw.mail.Email;
import feign.form.FormData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailWriterAdapter implements EmailWriter {
    private final GoaSupportClient goaSupportClient;

    @Override
    public void sendHtml(Email email, byte[] attachment, String fileName) {
        try {
            FormData formData = new FormData("application/pdf", fileName, attachment);
            goaSupportClient.sendHtml(email, formData);
        } catch (Exception exception) {
            throw new BadGatewayException(exception.getMessage() + " on sendHtml", exception.getCause());
        }
    }
}
