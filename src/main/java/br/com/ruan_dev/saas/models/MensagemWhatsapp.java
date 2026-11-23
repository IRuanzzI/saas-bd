package br.com.ruan_dev.saas.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Mensagem que o sistema enviou a um cliente pelo WhatsApp (criação do
 * agendamento ou lembrete). Guardamos o ID dela: quando o cliente responde
 * "respondendo" a essa mensagem, o WhatsApp devolve esse ID, e assim sabemos de
 * quem é a resposta mesmo sem o número de telefone.
 */
@Entity
@Table(name = "mensagem_whatsapp", indexes = @Index(columnList = "tenantId, messageId"))
public class MensagemWhatsapp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tenantId;
    private String messageId;
    private Long agendamentoId;
    private Long clienteId;

    /** CRIACAO ou LEMBRETE. */
    private String tipo;

    private LocalDateTime criadoEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public Long getAgendamentoId() { return agendamentoId; }
    public void setAgendamentoId(Long agendamentoId) { this.agendamentoId = agendamentoId; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
}
