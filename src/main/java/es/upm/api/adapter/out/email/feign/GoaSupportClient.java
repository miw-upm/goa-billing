package es.upm.api.adapter.out.email.feign;

import es.upm.api.configurations.FeignConfig;
import es.upm.miw.mail.Email;
import feign.form.FormData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;

@FeignClient(name = GoaSupportClient.GOA_SUPPORT, configuration = FeignConfig.class)
public interface GoaSupportClient {
    String GOA_SUPPORT = "goa-support";
    String EMAILS = "/emails";
    String HTML = "/html";
    String ATTACHMENT = "/attachment";

    @PostMapping(value = EMAILS + HTML + ATTACHMENT, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    void sendHtml(
            @RequestPart("email") Email email,
            @RequestPart("attachment") FormData attachment);
}
