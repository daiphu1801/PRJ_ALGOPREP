package com.algoprep.identity.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Plain-text mail through the configured sender with the configured From address. */
@SuppressWarnings("unchecked")
class SmtpEmailAdapterTest {
  @Test
  void sendsPlainTextMessageWithConfiguredSender() {
    JavaMailSender sender = mock(JavaMailSender.class);
    ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
    when(provider.getObject()).thenReturn(sender);
    new SmtpEmailAdapter(provider, "noreply@algoprep.test").send("a@b.c", "Subject", "Body");
    ArgumentCaptor<SimpleMailMessage> cap = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(sender).send(cap.capture());
    assertThat(cap.getValue().getFrom()).isEqualTo("noreply@algoprep.test");
    assertThat(cap.getValue().getTo()).containsExactly("a@b.c");
    assertThat(cap.getValue().getSubject()).isEqualTo("Subject");
    assertThat(cap.getValue().getText()).isEqualTo("Body");
  }
}
