package br.com.ruan_dev.saas.controllers;

import br.com.ruan_dev.saas.services.RespostaWhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    @Autowired
    private RespostaWhatsAppService respostaService;

    /** Se preenchido, só aceita chamadas com ?token=<segredo> (evita respostas forjadas). */
    @Value("${whatsapp.webhook.secret:}")
    private String webhookSecret;

    @PostMapping("/evolution")
    public ResponseEntity<Void> receberMensagemWhatsApp(@RequestBody JsonNode payload,
                                                        @RequestParam(required = false) String token) {
        if (webhookSecret != null && !webhookSecret.isBlank() && !webhookSecret.equals(token)) {
            return ResponseEntity.status(401).build();
        }
        try {
            // 1. Só interessam mensagens recebidas
            if (!"messages.upsert".equals(texto(payload, "event"))) {
                return ResponseEntity.ok().build();
            }
            JsonNode data = payload.path("data");
            JsonNode key = data.path("key");

            // 2. Ignora mensagens enviadas pelo próprio número da clínica/sistema
            if (key.path("fromMe").asBoolean()) {
                return ResponseEntity.ok().build();
            }

            // 3. Ignora grupos e status
            String remoteJid = texto(key, "remoteJid");
            if (remoteJid == null || remoteJid.contains("@g.us") || remoteJid.startsWith("status@")) {
                return ResponseEntity.ok().build();
            }
            // Evolution v2: quando o remetente vem como LID, informa o número real em remoteJidAlt.
            if (remoteJid.endsWith("@lid")) {
                String alt = texto(key, "remoteJidAlt");
                if (alt != null && alt.endsWith("@s.whatsapp.net")) {
                    remoteJid = alt;
                }
            }

            // 4. Texto da mensagem (comum ou respondendo a outra)
            String conteudo = texto(data.path("message"), "conversation");
            if (conteudo == null) {
                conteudo = texto(data.path("message").path("extendedTextMessage"), "text");
            }
            if (conteudo == null) {
                return ResponseEntity.ok().build();
            }

            // 5. ID da mensagem que o cliente respondeu (se ele respondeu "citando" uma)
            String citada = texto(data.path("contextInfo"), "stanzaId");
            if (citada == null) {
                citada = texto(data.path("message").path("extendedTextMessage").path("contextInfo"), "stanzaId");
            }

            String instancia = texto(payload, "instance");
            String pushName = texto(data, "pushName");

            // 6. Descobre de quem é a resposta e atualiza o agendamento
            String resultado = respostaService.processar(instancia, remoteJid, pushName, conteudo, citada);
            System.out.println("WEBHOOK WHATSAPP - Instância: " + instancia + " | De: " + remoteJid
                    + " (" + pushName + ") | Texto: [" + conteudo.trim() + "] -> " + resultado);
        } catch (Exception e) {
            System.err.println("Erro ao processar webhook do WhatsApp: " + e.getMessage());
        }

        // SEMPRE retorna 200 OK de imediato para a Evolution API destravar o chat
        return ResponseEntity.ok().build();
    }

    private static String texto(JsonNode no, String campo) {
        if (no == null) return null;
        JsonNode valor = no.get(campo);
        if (valor == null || valor.isNull() || valor.isMissingNode()) return null;
        String t = valor.asString();
        return t == null || t.isBlank() ? null : t;
    }
}
