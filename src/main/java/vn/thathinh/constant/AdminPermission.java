package vn.thathinh.constant;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

@Getter
public enum AdminPermission {
    DASHBOARD_VIEW,
    USER_VIEW,
    USER_MANAGE,
    TOPIC_MANAGE,
    REPORT_VIEW,
    REPORT_MANAGE;

    public enum Preset {
        SUPER_ADMIN,
        MODERATOR
    }

    public static Set<AdminPermission> presetPermissions(Preset preset) {
        return switch (preset) {
            case SUPER_ADMIN -> EnumSet.allOf(AdminPermission.class);
            case MODERATOR -> EnumSet.of(DASHBOARD_VIEW, USER_VIEW, REPORT_VIEW, REPORT_MANAGE, TOPIC_MANAGE);
        };
    }
}
