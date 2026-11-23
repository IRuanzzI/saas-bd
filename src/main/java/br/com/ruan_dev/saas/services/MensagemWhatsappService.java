package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.models.MensagemWhatsapp;
import br.com.ruan_dev.saas.repositories.MensagemWhatsappRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MensagemWhatsappService {

    public static final String CRIACAO = "CRIACAO";
    public static final String LEMBRETE = "LEMBRETE";

    @Autowired
    private MensagemWhatsappRepository repository;

    /** Guarda o ID de uma mensagem enviada ao cliente (ignora se o envio falhou e não há ID). */
    public void registrarEnvio(Long tenantId, String messageId, Long agendamentoId, Long clienteId, String tipo) {
        if (messageId == null || messageId.isBlank()) return;
        try {
            MensagemWhatsapp m = new MensagemWhatsapp();
            m.setTenantId(tenantId);
            m.setMessageId(messageId);
            m.setAgendamentoId(agendamentoId);
            m.setClienteId(clienteId);
            m.setTipo(tipo);
            m.setCriadoEm(LocalDateTime.now());
            repository.save(m);
        } catch (Exception e) {
            // Guardar o ID é um reforço: nunca pode atrapalhar o envio.
            System.err.println("Não foi possível guardar o ID da mensagem enviada: " + e.getMessage());
        }
    }
}
