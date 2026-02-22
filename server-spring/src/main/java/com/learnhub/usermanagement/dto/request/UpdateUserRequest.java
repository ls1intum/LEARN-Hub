package com.learnhub.usermanagement.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    @JsonProperty("first_name")
    private String firstName;
    
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    @JsonProperty("last_name")
    private String lastName;
    
    @Pattern(regexp = "^(TEACHER|ADMIN)$", message = "Role must be either TEACHER or ADMIN")
    private String role;
    
    @Size(min = 8, max = 255, message = "Password must be between 8 and 255 characters")
    private String password;
}
