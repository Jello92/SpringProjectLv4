package com.example.board_spring3.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Pattern;

@Setter
@Getter
@AllArgsConstructor
public class UserRequestDto {
    @Pattern (regexp = "^[0-9a-z]{4,10}$", message = "4 ~ 10자 사이의 알파벳 소문자와 숫자만 가능합니다.")
    private String username;

    private String password;

    private boolean admin = false;

    private String adminToken = "";
}
