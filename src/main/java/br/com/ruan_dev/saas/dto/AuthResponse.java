package br.com.ruan_dev.saas.dto;

public class AuthResponse {
    private String token;
    private String nome;
    private String role;
    private Long tenantId;
    private String tenant;
    private Long usuarioId;
    private boolean precisaTrocarSenha;

    public AuthResponse(String token, String nome, String role, Long tenantId,
                        String tenant, Long usuarioId, boolean precisaTrocarSenha) {
        this.token = token;
        this.nome = nome;
        this.role = role;
        this.tenantId = tenantId;
        this.tenant = tenant;
        this.usuarioId = usuarioId;
        this.precisaTrocarSenha = precisaTrocarSenha;
    }

    // Sem os getters o Jackson não serializa os campos e o login volta vazio.
    public String getToken() { return token; }
    public String getNome() { return nome; }
    public String getRole() { return role; }
    public Long getTenantId() { return tenantId; }
    public String getTenant() { return tenant; }
    public Long getUsuarioId() { return usuarioId; }
    public boolean isPrecisaTrocarSenha() { return precisaTrocarSenha; }
}
