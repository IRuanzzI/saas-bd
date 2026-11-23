package br.com.ruan_dev.saas.models;

//O CLIENTE DO USUARIO

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.context.annotation.ComponentScan;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "Cliente")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A empresa não entra nem sai do JSON: vem sempre do token.
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    private String nome;
    private String telefone;
    private String email;
    private String ultimoAtendimento;
    private String clienteDesde;

    // Identificador interno (LID) que o WhatsApp usa para este contato, aprendido
    // na primeira resposta. Serve para reconhecer o cliente quando a mensagem
    // chega sem o número de telefone. Não vai no JSON.
    @JsonIgnore
    private String whatsappLid;

    public String getWhatsappLid() {
        return whatsappLid;
    }

    public void setWhatsappLid(String whatsappLid) {
        this.whatsappLid = whatsappLid;
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

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUltimoAtendimento() {
        return ultimoAtendimento;
    }

    public void setUltimoAtendimento(String ultimoAtendimento) {
        this.ultimoAtendimento = ultimoAtendimento;
    }

    public String getClienteDesde() {
        return clienteDesde;
    }

    public void setClienteDesde(String clienteDesde) {
        this.clienteDesde = clienteDesde;
    }
}
