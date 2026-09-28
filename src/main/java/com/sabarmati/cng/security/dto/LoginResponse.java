package com.sabarmati.cng.security.dto;

public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private String role;

    public LoginResponse() {
    }

    public LoginResponse(String token, String tokenType, String role) {
        this.token = token;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.role = role;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String token;
        private String tokenType = "Bearer";
        private String role;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder tokenType(String tokenType) {
            if (tokenType != null) this.tokenType = tokenType;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public LoginResponse build() {
            return new LoginResponse(token, tokenType, role);
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
