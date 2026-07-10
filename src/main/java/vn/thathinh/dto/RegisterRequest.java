package vn.thathinh.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import vn.thathinh.constant.Gender;

import java.time.LocalDate;

@Data
public class RegisterRequest {
    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6, max = 100)
    private String password;

    @NotBlank @Size(min = 3, max = 20)
    @Pattern(regexp = "^[a-zA-Z0-9_\\u00C0-\\u1EF9]+$", message = "Nickname chỉ chứa chữ, số và gạch dưới")
    private String nickname;

    @NotNull(message = "Giới tính không được để trống")
    private Gender gender;

    @NotNull(message = "Ngày sinh không được để trống")
    private LocalDate birthDate;
}
