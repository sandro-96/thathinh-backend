package vn.thathinh.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.thathinh.constant.AdminPermission;
import vn.thathinh.constant.UserRole;
import vn.thathinh.model.User;
import vn.thathinh.repository.UserRepository;

import java.util.EnumSet;

@Slf4j
@Configuration
public class AdminSeedConfig {

    @Bean
    ApplicationRunner seedAdmin(UserRepository userRepository,
                                PasswordEncoder encoder,
                                Environment environment,
                                @Value("${app.admin.email:admin@thathinh.vn}") String adminEmail,
                                @Value("${app.admin.password:}") String adminPassword,
                                @Value("${app.admin.seed-enabled:false}") boolean seedEnabled) {
        return args -> {
            boolean dev = environment.getActiveProfiles().length == 0
                    || environment.matchesProfiles("dev");

            if (!dev && !seedEnabled) {
                return;
            }

            String password = adminPassword;
            if (password == null || password.isBlank()) {
                if (dev) {
                    password = "Admin@123";
                } else {
                    log.warn("Bỏ qua seed admin: ADMIN_PASSWORD chưa được đặt (production yêu cầu mật khẩu mạnh).");
                    return;
                }
            }

            if (userRepository.findByEmailAndDeletedFalse(adminEmail).isEmpty()) {
                User admin = User.builder()
                        .email(adminEmail)
                        .password(encoder.encode(password))
                        .nickname("admin")
                        .verified(true)
                        .role(UserRole.ROLE_ADMIN)
                        .active(true)
                        .adminPermissions(EnumSet.allOf(AdminPermission.class))
                        .build();
                userRepository.save(admin);
                log.info("Đã tạo tài khoản admin mặc định: {}", adminEmail);
            }
        };
    }
}
