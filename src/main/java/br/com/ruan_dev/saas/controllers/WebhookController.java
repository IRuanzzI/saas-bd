package br.com.ruan_dev.saas.controllers;

import br.com.ruan_dev.saas.services.AgendamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    @Autowired
    private AgendamentoService agendamentoService;

    @PostMapping("/evolution")
    public ResponseEntity<Void> receberMensagemWhatsApp(@RequestBody JsonNode payload) {
        try {
            // 1. Valida se o evento é de mensagens
            if (payload.has("event") && "messages.upsert".equals(payload.get("event").asText())) {
                JsonNode data = payload.get("data");

                // 2. Ignora mensagens enviadas pelo próprio robô/sistema
                if (data.path("key").path("fromMe").asBoolean()) {
                    return ResponseEntity.ok().build();
                }

                // 3. Extrai dados básicos
                String remoteJid = data.path("key").path("remoteJid").asText("");

                // Ignora mensagens de grupos do WhatsApp
                if (remoteJid.contains("@g.us")) {
                    return ResponseEntity.ok().build();
                }

                String telefoneCliente = remoteJid.split("@")[0];
                String nomeInstancia = payload.path("instance").asText("");

                // 4. Extrai o texto da mensagem (suporta texto comum e respondido)
                String texto = data.path("message").path("conversation").asText("");
                if (texto.isEmpty()) {
                    texto = data.path("message").path("extendedTextMessage").path("text").asText("");
                }

                if (texto.isEmpty()) {
                    return ResponseEntity.ok().build();
                }

                String textoLimpo = texto.trim().toUpperCase();
                System.out.println("WEBHOOK RECEBIDO - Instância: " + nomeInstancia + " | Tel: " + telefoneCliente + " | Texto: [" + textoLimpo + "]");

                // 5. Executa a ação correspondente no banco
                if (textoLimpo.equals("N") || textoLimpo.equals("NAO") || textoLimpo.equals("NÃO") || textoLimpo.equals("CANCELAR")) {
                    agendamentoService.atualizarStatusAgendamento(telefoneCliente, "CANCELADO");
                } else if (textoLimpo.equals("S") || textoLimpo.equals("SIM")) {
                    agendamentoService.atualizarStatusAgendamento(telefoneCliente, "CONFIRMADO");
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar webhook do WhatsApp: " + e.getMessage());
        }

        // SEMPRE retorna 200 OK de imediato para a Evolution API destravar o chat
        return ResponseEntity.ok().build();
    }
}