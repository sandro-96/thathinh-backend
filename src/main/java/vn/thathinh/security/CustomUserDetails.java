package vn.thathinh.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.thathinh.constant.AdminPermission;
import vn.thathinh.constant.UserRole;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

@Getter
public class CustomUserDetails implements UserDetails {
    private final String userId;
    private final String email;
    private final String password;
    private final UserRole role;
    private final Set<AdminPermission> adminPermissions;
    private final Collection<? extends GrantedAuthority> authorities;

    @Setter
    private String impersonatedBy;

    public CustomUserDetails(String userId, String email, String password, UserRole role,
                             Set<AdminPermission> adminPermissions,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.adminPermissions = adminPermissions != null ? adminPermissions : EnumSet.noneOf(AdminPermission.class);
        this.authorities = authorities;
    }

    @Override public String getUsername() { return userId; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
