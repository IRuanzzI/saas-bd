package br.com.ruan_dev.saas.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Registro de ações sensíveis (quem fez o quê, em qual empresa). */
@Entity
@Table(name = "auditoria_log")
public class AuditoriaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Empresa afetada pela ação. */
    private Long tenantAlvoId;

    /** E-mail de quem executou a ação. */
    private String ator;

    private String acao;

    @Column(length = 500)
    private String detalhe;

    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantAlvoId() { return tenantAlvoId; }
    public void setTenantAlvoId(Long tenantAlvoId) { this.tenantAlvoId = tenantAlvoId; }
    public String getAtor() { return ator; }
    public void setAtor(String ator) { this.ator = ator; }
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }
    public String getDetalhe() { return detalhe; }
    public void setDetalhe(String detalhe) { this.detalhe = detalhe; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
