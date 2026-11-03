package br.com.ruan_dev.saas.dto;

public class AuthResponse {
    private String token;
    private String nome;
    private String role;
    private Long tenantId;

    public AuthResponse(String token, String nome, String role, Long tenantId) {
        this.token = token;
        this.nome = nome;
        this.role = role;
        this.tenantId = tenantId;
    }
    // Getters
}