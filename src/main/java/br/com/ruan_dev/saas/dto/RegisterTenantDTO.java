package br.com.ruan_dev.saas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterTenantDTO {

    @NotBlank(message = "O nome da empresa é obrigatório")
    private String nomeFantasia;

    @NotBlank(message = "O CNPJ/CPF é obrigatório")
    private String documento;

    @NotBlank(message = "O nome do administrador é obrigatório")
    private String adminNome;

    @Email(message = "E-mail inválido")
    @NotBlank(message = "O e-mail é obrigatório")
    private String adminEmail;

    // Senha provisória: o admin é obrigado a trocá-la no primeiro acesso.
    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, message = "A senha provisória precisa ter pelo menos 8 caracteres")
    private String adminSenha;

    // Opcionais
    private String plano;
    /** yyyy-MM-dd */
    private String venceEm;


    public String getNomeFantasia() { return nomeFantasia; }
    public String getDocumento() { return documento; }
    public String getAdminNome() { return adminNome; }
    public String getAdminEmail() { return adminEmail; }
    public String getAdminSenha() { return adminSenha; }
    public String getPlano() { return plano; }
    public String getVenceEm() { return venceEm; }


    public void setNomeFantasia(String nomeFantasia) { this.nomeFantasia = nomeFantasia; }
    public void setDocumento(String documento) { this.documento = documento; }
    public void setAdminNome(String adminNome) { this.adminNome = adminNome; }
    public void setAdminEmail(String adminEmail) { this.adminEmail = adminEmail; }
    public void setAdminSenha(String adminSenha) { this.adminSenha = adminSenha; }
    public void setPlano(String plano) { this.plano = plano; }
    public void setVenceEm(String venceEm) { this.venceEm = venceEm; }
}
