package com.eaglepoint.workforce.dto;

import com.eaglepoint.workforce.enums.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public class UserCreateRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;

    @Size(max = 100)
    private String displayName;

    @Size(max = 100)
    private String email;

    private Set<RoleName> roles;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Set<RoleName> getRoles() { return roles; }
    public void setRoles(Set<RoleName> roles) { this.roles = roles; }
}
