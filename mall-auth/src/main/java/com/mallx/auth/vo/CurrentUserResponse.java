package com.mallx.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录账号的安全展示信息。
 */
@Data
@AllArgsConstructor
public class CurrentUserResponse {

    private Long userId;

    private String username;
}
