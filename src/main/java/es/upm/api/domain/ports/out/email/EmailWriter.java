package es.upm.api.domain.ports.out.email;

import es.upm.miw.mail.Email;

public interface EmailWriter {
    void sendHtml(Email email, byte[] attachment, String fileName);
}
