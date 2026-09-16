package com.sitms.notification;

import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Sends event-emails to notification recipients (SITMS-053, EPIC 12).
 *
 * <p>The SMTP channel is opt-in: when {@code sitms.mail.enabled=false} (the default, and the test
 * profile) or no {@code spring.mail.host} is configured, sending is skipped entirely so local and
 * CI runs are never blocked on a mail server. The recipient's address comes from the shared
 * {@code app_user} table; recipients without an account (or without an address) are skipped.
 * Failures are logged, never propagated to the caller, so a broken SMTP setup cannot roll back the
 * domain operation that triggered the notification.
 */
@Component
public class EmailNotificationSender {

  private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

  private final boolean enabled;
  private final String from;
  private final JavaMailSender mailSender;
  private final JdbcTemplate jdbcTemplate;

  public EmailNotificationSender(
      @Value("${sitms.mail.enabled:false}") boolean enabled,
      @Value("${sitms.mail.from:no-reply@sitms.local}") String from,
      JavaMailSender mailSender,
      JdbcTemplate jdbcTemplate) {
    this.enabled = enabled;
    this.from = from;
    this.mailSender = mailSender;
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Emails a recipient about a notification. Requires the feature flag AND a configured sender;
   * otherwise this is a no-op.
   */
  public void send(UUID recipientId, NotificationType type, String title, String body) {
    if (!enabled) {
      return;
    }
    if (!configured()) {
      log.warn(
          "Email notifications enabled but no spring.mail.host configured; skipping email for {}.",
          type);
      return;
    }
    String recipientEmail = emailOf(recipientId);
    if (recipientEmail == null) {
      log.debug("No app_user email for {}; skipping email for {}.", recipientId, type);
      return;
    }
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
      helper.setFrom(from);
      helper.setTo(recipientEmail);
      helper.setSubject("SITMS: " + title);
      helper.setText(text(title, body));
      mailSender.send(message);
    } catch (Exception e) {
      log.error("Failed to send notification email for {} to {}.", type, recipientEmail, e);
    }
  }

  private boolean configured() {
    return mailSender != null;
  }

  private String emailOf(UUID recipientId) {
    List<String> matches =
        jdbcTemplate.query(
            "SELECT email FROM app_user WHERE id = ?",
            (rs, rowNum) -> rs.getString(1),
            recipientId);
    return matches.isEmpty() ? null : matches.get(0);
  }

  private static String text(String title, String body) {
    StringBuilder text = new StringBuilder(title).append("\n\n");
    if (body != null && !body.isBlank()) {
      text.append(body).append("\n\n");
    }
    text.append("— SITMS");
    return text.toString();
  }
}