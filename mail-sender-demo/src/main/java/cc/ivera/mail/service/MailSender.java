package cc.ivera.mail.service;

import cc.ivera.mail.model.MailMessage;
import jakarta.mail.MessagingException;

import java.io.IOException;

public interface MailSender {

    void send(MailMessage message) throws MessagingException, IOException;
}
