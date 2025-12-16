package com.learnhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private UserResponse user;

    public LoginResponse(String accessToken, UserResponse user) {
        this.accessToken = accessToken;
        this.user = user;
    }
}
