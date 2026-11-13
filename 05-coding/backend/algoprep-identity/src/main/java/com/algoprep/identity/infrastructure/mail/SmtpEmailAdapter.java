package com.algoprep.identity.infrastructure.mail;

import com.algoprep.identity.application.ports.out.EmailPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** SMTP transport; the local profile delivers to Mailpit. Never logs message content. */
@Component
class SmtpEmailAdapter implements EmailPort {
  private final ObjectProvider<JavaMailSender> sender;
  private final String from;

  SmtpEmailAdapter(
      ObjectProvider<JavaMailSender> sender,
      @Value("${algoprep.mail.from:noreply@algoprep.local}") String from) {
    this.sender = sender;
    this.from = from;
  }

  @Override
  public void send(String recipient, String subject, String body) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(recipient);
    message.setSubject(subject);
    message.setText(body);
    sender.getObject().send(message);
  }
}
