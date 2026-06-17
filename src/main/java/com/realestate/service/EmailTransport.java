package com.realestate.service;

/**
 * Low-level email delivery. Implementations are selected by Spring profile:
 *   - {@link SmtpEmailTransport}    (dev / !prod) — JavaMailSender over SMTP (Mailtrap).
 *   - {@link SendGridEmailTransport} (prod)       — SendGrid Web API over HTTPS.
 *
 * Render blocks outbound SMTP ports (25/465/587), so prod must use the HTTPS API.
 * Implementations must never throw — email failures are logged, not propagated.
 */
public interface EmailTransport {
    void send(String to, String subject, String htmlBody);
}
