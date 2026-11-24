package com.movento.userservice.account;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Sends account emails over SMTP. Locally that is Mailpit (http://localhost:8025), so no real email leaves the machine.
 * A mail failure never fails the request: the user can ask for a new link.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountMailer {
    private final JavaMailSender mailSender;

    @Value("${app.web-url:http://localhost:3000}")
    private String webUrl;

    @Value("${app.mail.from:Movento <no-reply@movento.local>}")
    private String from;

    public void sendVerification(String to, String firstName, String token) {
        send(to, "Confirm your Movento email",
                "Hi " + firstName + ",\n\nConfirm your email address to finish setting up your Movento account:\n\n"
                        + link("/verify-email", token) + "\n\nThe link expires in 24 hours.\n\n— Movento");
    }

    public void sendPasswordReset(String to, String firstName, String token) {
        send(to, "Reset your Movento password",
                "Hi " + firstName + ",\n\nWe received a request to reset your password. Choose a new one here:\n\n"
                        + link("/reset-password", token)
                        + "\n\nThe link expires in 30 minutes. If you didn't ask for this, you can ignore this email.\n\n— Movento");
    }

    private String link(String path, String token) {
        return webUrl + path + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    private void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Could not send '{}' email: {}", subject, ex.getMessage());
        }
    }
}
