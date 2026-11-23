package br.com.ruan_dev.saas.models;

//Dono do negocio

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Entity
@Table(name = "Usuario")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A empresa não entra nem sai do JSON: vem sempre do token.
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    private String nome;

    // Nunca devolver o hash da senha nas respostas.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;

    // O login busca por e-mail: não pode haver dois usuários com o mesmo.
    @Column(unique = true)
    private String email;

    private String role;

    // Senha provisória (cadastro ou redefinição): precisa ser trocada no
    // próximo acesso. Nulo conta como "não".
    @JsonIgnore
    private Boolean precisaTrocarSenha;

    public Boolean getPrecisaTrocarSenha() {
        return precisaTrocarSenha;
    }

    public void setPrecisaTrocarSenha(Boolean precisaTrocarSenha) {
        this.precisaTrocarSenha = precisaTrocarSenha;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
