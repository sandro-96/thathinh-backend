package vn.thathinh.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.thathinh.model.User;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.brand.name:Thả Thính}")
    private String brandName;

    public void sendVerificationEmail(User user) {
        String link = frontendUrl + "/verify-email?token=" + user.getVerificationToken();
        String subject = brandName + " - Xác minh email";
        String html = """
                <p>Xin chào <b>%s</b>,</p>
                <p>Nhấn vào liên kết bên dưới để xác minh email và bắt đầu dùng %s:</p>
                <p><a href="%s">Xác minh email</a></p>
                <p>Liên kết hết hạn sau 24 giờ.</p>
                """.formatted(user.getNickname(), brandName, link);

        if (!StringUtils.hasText(mailFrom)) {
            log.info("[DEV] Verification link for {}: {}", user.getEmail(), link);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), ex.getMessage());
            log.info("[FALLBACK] Verification link: {}", link);
        }
    }
}
