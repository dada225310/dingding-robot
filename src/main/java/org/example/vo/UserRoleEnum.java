package org.example.vo;

public enum UserRoleEnum {
    USER("user"),
    ASSISTANT("assistant");
    private String role;
    UserRoleEnum(String role) {
        this.role = role;
    }
    public String getRole() {
        return role;
    }
}
