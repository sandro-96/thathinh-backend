package vn.thathinh.constant;

import lombok.Getter;

@Getter
public enum ApiCode {
    SUCCESS("2000", "Thành công"),
    EMAIL_SENT("2001", "Email đã được gửi"),
    CREATED("2002", "Tạo thành công"),

    VALIDATION_ERROR("4000", "Dữ liệu không hợp lệ"),
    UNAUTHORIZED("4001", "Chưa đăng nhập"),
    ACCESS_DENIED("4003", "Không có quyền truy cập"),
    NOT_FOUND("4004", "Không tìm thấy"),
    BAD_REQUEST("4005", "Yêu cầu không hợp lệ"),

    EMAIL_ALREADY_EXISTS("4100", "Email đã tồn tại"),
    NICKNAME_ALREADY_EXISTS("4101", "Nickname đã được sử dụng"),
    NICKNAME_INVALID("4108", "Nickname không hợp lệ (3–20 ký tự, chữ/số/gạch dưới)"),
    NICKNAME_BANNED_WORD("4109", "Nickname chứa từ không được phép"),
    INVALID_CREDENTIALS("4102", "Email hoặc mật khẩu không đúng"),
    EMAIL_NOT_VERIFIED("4103", "Email chưa được xác minh"),
    INVALID_TOKEN("4104", "Token không hợp lệ hoặc đã hết hạn"),
    USER_NOT_FOUND("4105", "Không tìm thấy người dùng"),
    USER_BANNED("4106", "Tài khoản đã bị khoá"),
    PROFILE_INCOMPLETE("4107", "Vui lòng hoàn thiện hồ sơ"),
    USER_BLOCKED("4110", "Không thể tương tác với người dùng này"),
    USER_ALREADY_BLOCKED("4111", "Đã chặn người dùng này"),
    USER_NOT_BLOCKED("4112", "Chưa chặn người dùng này"),

    TOPIC_NOT_FOUND("4200", "Không tìm thấy topic"),
    TOPIC_NOT_MEMBER("4201", "Bạn chưa tham gia topic này"),
    TOPIC_ALREADY_JOINED("4202", "Bạn đã tham gia topic này"),
    TOPIC_INACTIVE("4203", "Topic không còn hoạt động"),

    FLIRT_ALREADY_WAITING("4300", "Bạn đang chờ ghép đôi"),
    FLIRT_NOT_IN_SESSION("4301", "Không có phiên thả thính"),
    FLIRT_SESSION_NOT_FOUND("4302", "Không tìm thấy phiên thả thính"),
    FLIRT_SESSION_ENDED("4303", "Phiên thả thính đã kết thúc"),
    FLIRT_NO_MATCH("4304", "Chưa tìm được đối tác"),

    MESSAGE_EMPTY("4400", "Tin nhắn không được để trống"),
    MESSAGE_TOO_LONG("4401", "Tin nhắn quá dài"),
    MESSAGE_BANNED_WORD("4402", "Tin nhắn chứa từ không được phép"),

    RATE_LIMIT_MESSAGE("4500", "Bạn gửi tin quá nhanh, vui lòng thử lại sau"),
    RATE_LIMIT_FLIRT("4501", "Bạn thả thính quá nhiều, thử lại sau một lúc"),
    RATE_LIMIT_AUTH("4502", "Quá nhiều yêu cầu, vui lòng thử lại sau ít phút"),
    RATE_LIMIT_EMAIL("4503", "Bạn yêu cầu email quá nhiều, thử lại sau một lúc"),

    FRIEND_ALREADY_EXISTS("4600", "Đã là bạn hoặc đang chờ xác nhận"),
    FRIEND_REQUEST_PENDING("4601", "Lời mời kết bạn đang chờ phản hồi"),
    FRIEND_NOT_FOUND("4602", "Không tìm thấy lời mời kết bạn"),
    FRIEND_NOT_ACCEPTED("4603", "Chưa kết bạn với người này"),
    FRIEND_REQUEST_NOT_FOUND("4604", "Không tìm thấy lời mời"),
    FRIENDSHIP_NOT_FOUND("4605", "Không tìm thấy quan hệ bạn bè"),
    FLIRT_HISTORY_ALREADY_IMPORTED("4606", "Lịch sử flirt đã được chuyển sang chat riêng"),
    FLIRT_HISTORY_NOT_AVAILABLE("4607", "Không có lịch sử flirt để chuyển"),

    CONVERSATION_NOT_FOUND("4700", "Không tìm thấy cuộc trò chuyện"),
    CONVERSATION_NOT_PARTICIPANT("4701", "Bạn không tham gia cuộc trò chuyện này"),

    LOCATION_NOT_ENABLED("4800", "Vui lòng bật chia sẻ vị trí để tìm quanh đây"),

    INTERNAL_ERROR("5000", "Lỗi hệ thống");

    private final String code;
    private final String message;

    ApiCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
