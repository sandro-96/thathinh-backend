package vn.thathinh.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.thathinh.constant.ApiCode;
import vn.thathinh.constant.UserRole;
import vn.thathinh.dto.ApiResponseDto;
import vn.thathinh.model.User;
import vn.thathinh.repository.UserRepository;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.isTokenValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String userId = jwtUtil.extractUserId(token);

                var userOpt = userRepository.findById(userId);
                if (userOpt.isEmpty() || userOpt.get().isDeleted()) {
                    chain.doFilter(request, response);
                    return;
                }
                User user = userOpt.get();

                if ((!user.isActive() || user.isBanned()) && isProtectedApi(request.getRequestURI())) {
                    writeError(response, ApiCode.USER_BANNED);
                    return;
                }

                // Nguồn sự thật là DB, không phải claim trong token: user bị hạ quyền/đổi vai trò
                // sẽ có hiệu lực ngay ở request kế tiếp thay vì đợi token hết hạn.
                UserRole role = user.getRole();
                var perms = EnumSet.noneOf(vn.thathinh.constant.AdminPermission.class);
                if (role == UserRole.ROLE_ADMIN && user.getAdminPermissions() != null) {
                    perms.addAll(user.getAdminPermissions());
                }

                CustomUserDetails details = new CustomUserDetails(
                        userId, user.getEmail(), null, role, perms,
                        List.of(new SimpleGrantedAuthority(role.name()))
                );
                var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isProtectedApi(String uri) {
        return uri.startsWith("/api/") && !uri.startsWith("/api/auth/");
    }

    private void writeError(HttpServletResponse response, ApiCode code) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponseDto.error(code)));
    }
}
