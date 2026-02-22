package com.learnhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private String error;
    
    public static ErrorResponse of(String message) {
        return new ErrorResponse(message);
    }
}
