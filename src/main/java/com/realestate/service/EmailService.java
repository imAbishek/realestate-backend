package com.realestate.service;

import com.realestate.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails.
 * Methods are @Async so they don't slow down the HTTP response —
 * email sending happens in a background thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender  mailSender;
    private final AppProperties   appProperties;

    // ─────────────────────────────────────────────
    // Email verification OTP
    // ─────────────────────────────────────────────

    @Async
    public void sendVerificationEmail(String toEmail, String name, String otp) {
        String subject = "Verify your email — PropFind";
        String body = """
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;">
              <h2 style="color: #185FA5;">Welcome to PropFind, %s!</h2>
              <p>Your email verification code is:</p>
              <div style="background: #f4f4f4; border-radius: 8px; padding: 20px;
                          text-align: center; font-size: 32px; font-weight: bold;
                          letter-spacing: 8px; color: #185FA5; margin: 20px 0;">
                %s
              </div>
              <p style="color: #666;">This code expires in <strong>15 minutes</strong>.</p>
              <p style="color: #666; font-size: 12px;">
                If you didn't create a PropFind account, ignore this email.
              </p>
            </div>
            """.formatted(name, otp);

        sendHtmlEmail(toEmail, subject, body);
    }

    // ─────────────────────────────────────────────
    // Password reset
    // ─────────────────────────────────────────────

    @Async
    public void sendPasswordResetEmail(String toEmail, String name, String otp) {
        String subject = "Reset your password — PropFind";
        String body = """
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;">
              <h2 style="color: #185FA5;">Password Reset</h2>
              <p>Hi %s, your password reset code is:</p>
              <div style="background: #f4f4f4; border-radius: 8px; padding: 20px;
                          text-align: center; font-size: 32px; font-weight: bold;
                          letter-spacing: 8px; color: #D85A30; margin: 20px 0;">
                %s
              </div>
              <p style="color: #666;">This code expires in <strong>15 minutes</strong>.</p>
              <p style="color: #666; font-size: 12px;">
                If you didn't request a password reset, ignore this email.
                Your password has not been changed.
              </p>
            </div>
            """.formatted(name, otp);

        sendHtmlEmail(toEmail, subject, body);
    }

    // ─────────────────────────────────────────────
    // Inquiry notification (seller gets this when someone inquires)
    // ─────────────────────────────────────────────

    @Async
    public void sendInquiryNotification(
            String toEmail, String ownerName,
            String propertyTitle, String inquirerName, String inquirerPhone) {

        String subject = "New inquiry on your property — PropFind";
        String body = """
            <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;">
              <h2 style="color: #185FA5;">New inquiry received</h2>
              <p>Hi %s,</p>
              <p>Someone is interested in your property: <strong>%s</strong></p>
              <div style="background: #f4f4f4; border-radius: 8px; padding: 16px; margin: 20px 0;">
                <p style="margin:0;"><strong>Name:</strong> %s</p>
                <p style="margin:8px 0 0;"><strong>Phone:</strong> %s</p>
              </div>
              <p>Log in to PropFind to view the full message and reply.</p>
            </div>
            """.formatted(ownerName, propertyTitle, inquirerName, inquirerPhone);

        sendHtmlEmail(toEmail, subject, body);
    }

    // ─────────────────────────────────────────────
    // Core send method
    // ─────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
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
            log.debug("Email sent to: {} | Subject: {}", to, subject);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            // Log but don't throw — email failure shouldn't break the API response
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
