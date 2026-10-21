package com.dtaq.retail.util;

import com.dtaq.retail.config.MailConfig;
import com.dtaq.retail.exception.MailDeliveryException;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Gửi mã xác nhận đặt lại mật khẩu qua máy chủ SMTP đã cấu hình. */
public class SmtpMailer implements Mailer {

  private static final String SUBJECT = "DTAQ - Mã xác nhận đặt lại mật khẩu";

  public SmtpMailer() {
  }

  @Override
  public void sendPasswordResetCode(String toEmail, String code) {
    if (!MailConfig.isConfigured()) {
      throw new MailDeliveryException("Chưa cấu hình SMTP");
    }

    Properties properties = new Properties();
    properties.put("mail.smtp.host", MailConfig.host());
    properties.put("mail.smtp.port", Integer.toString(MailConfig.port()));
    properties.put("mail.smtp.auth", "true");
    properties.put("mail.smtp.starttls.enable", Boolean.toString(MailConfig.startTls()));
    properties.put("mail.smtp.connectiontimeout", Integer.toString(MailConfig.timeoutMs()));
    properties.put("mail.smtp.timeout", Integer.toString(MailConfig.timeoutMs()));

    Session session = Session.getInstance(properties, new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(MailConfig.user(), MailConfig.password());
      }
    });

    try {
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(MailConfig.from()));
      message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
      message.setSubject(SUBJECT, StandardCharsets.UTF_8.name());
      message.setText(body(code), StandardCharsets.UTF_8.name());
      Transport.send(message);
    } catch (MessagingException e) {
      throw new MailDeliveryException("Không thể gửi email xác nhận", e);
    }
  }

  private static String body(String code) {
    return "Mã xác nhận đặt lại mật khẩu DTAQ của bạn là: " + code
        + "\nMã có hiệu lực trong 5 phút. Nếu bạn không yêu cầu, hãy bỏ qua email này.";
  }
}
