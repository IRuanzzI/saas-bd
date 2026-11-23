package br.com.ruan_dev.saas.models;
import jakarta.persistence.*;

import java.time.LocalDate;

//A EMPRESA

@Entity
@Table(name = "Tenant")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome_fantasia;

    // CNPJ/CPF: não pode haver duas empresas com o mesmo documento.
    @Column(unique = true)
    private String documento;

    private Boolean ativo = false;

    // Plano contratado (texto livre: TESTE, BASICO, PROFISSIONAL...).
    private String plano;

    // Último dia de acesso. Depois dele a empresa é tratada como vencida.
    private LocalDate venceEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome_fantasia() {
        return nome_fantasia;
    }

    public void setNome_fantasia(String nome_fantasia) {
        this.nome_fantasia = nome_fantasia;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public String getPlano() {
        return plano;
    }

    public void setPlano(String plano) {
        this.plano = plano;
    }

    public LocalDate getVenceEm() {
        return venceEm;
    }

    public void setVenceEm(LocalDate venceEm) {
        this.venceEm = venceEm;
    }
}
