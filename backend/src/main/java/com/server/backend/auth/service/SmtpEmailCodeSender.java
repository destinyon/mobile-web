package com.server.backend.auth.service;

import com.server.backend.common.BusinessException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailCodeSender implements EmailCodeSender {
    private static final String DEFAULT_SUBJECT = "羽球在线登录验证码";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String from;
    private final String subject;
    private final boolean debug;

    public SmtpEmailCodeSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.email.from:${spring.mail.username:}}") String from,
            @Value("${app.email.subject:}") String subject,
            @Value("${app.email.debug:${EMAIL_DEBUG:false}}") boolean debug) {
        this.mailSenderProvider = mailSenderProvider;
        this.from = from;
        this.subject = subject == null || subject.isBlank() ? DEFAULT_SUBJECT : subject;
        this.debug = debug;
    }

    @Override
    public void send(String email, String code, long validSeconds) {
        if (debug) {
            return;
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || from == null || from.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "邮箱服务配置缺失");
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject(subject);
            message.setText("""
                    你的羽球在线登录验证码是：%s

                    验证码 %d 分钟内有效。若非本人操作，请忽略本邮件。
                    """.formatted(code, Math.max(1, validSeconds / 60)));
            mailSender.send(message);
        } catch (MailException ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "邮箱验证码发送失败");
        }
    }
}
