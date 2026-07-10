package vn.thathinh.config.ws;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.Principal;

@Getter
@RequiredArgsConstructor
public class StompPrincipal implements Principal {
    private final String userId;
    private final String role;

    @Override
    public String getName() {
        return userId;
    }
}
