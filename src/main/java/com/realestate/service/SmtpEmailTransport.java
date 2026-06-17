package com.realestate.service;

import com.realestate.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * SMTP email delivery for local/dev (e.g. Mailtrap). Not used in prod —
 * Render blocks outbound SMTP ports, so prod uses {@link SendGridEmailTransport}.
 */
@Component
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailTransport implements EmailTransport {

    private final JavaMailSender mailSender;
    private final AppProperties  appProperties;

    @Override
    public void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(
                appProperties.getMail().getFrom(),
                appProperties.getMail().getFromName()
            );
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);   // true = HTML

            mailSender.send(message);
            log.debug("Email sent via SMTP to: {} | Subject: {}", to, subject);

        } catch (MessagingException | java.io.UnsupportedEncodingException | MailException e) {
            // Log but don't throw — email failure shouldn't break the API response.
            log.error("Failed to send email via SMTP to {}: {}", to, e.getMessage());
        }
    }
}
