package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.dto.PainelDTOs.WhatsAppQr;
import br.com.ruan_dev.saas.dto.PainelDTOs.WhatsAppStatus;
import br.com.ruan_dev.saas.exceptions.ServicoIndisponivelException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Integração com a Evolution API (v2.3.x): envio de mensagens e gestão da
 * instância de WhatsApp de cada empresa (nome "tenant_<id>").
 */
@Service
public class WhatsAppService {

    @Value("${whatsapp.api.url:http://localhost:8081}")
    private String apiUrl;

    @Value("${whatsapp.api.token:ruan_saas_secret_key_2026}")
    private String apiKey;

    /** Endereço que a Evolution usa para avisar esta API das mensagens recebidas. */
    @Value("${whatsapp.webhook.url:http://host.docker.internal:8080/api/v1/webhooks/evolution}")
    private String webhookUrl;

    /** Se preenchido, o webhook só aceita chamadas com ?token=<segredo>. */
    @Value("${whatsapp.webhook.secret:}")
    private String webhookSecret;

    private final RestClient restClient;
    private final RestClient gestao;

    public WhatsAppService() {
        // Envio de mensagem: timeouts curtos (5s) para não travar o fluxo
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        // Gestão de instância: criar e gerar QR code pode demorar mais
        SimpleClientHttpRequestFactory fabricaGestao = new SimpleClientHttpRequestFactory();
        fabricaGestao.setConnectTimeout(5000);
        fabricaGestao.setReadTimeout(20000);
        this.gestao = RestClient.builder()
                .requestFactory(fabricaGestao)
                .build();
    }

    // ---------------------------------------------------------------- envio de mensagem

    /** Envia o texto e devolve o ID da mensagem no WhatsApp (ou null se o envio falhou). */
    public String enviarMensagemTexto(String nomeInstancia, String numeroTelefone, String texto) {
        String url = apiUrl + "/message/sendText/" + nomeInstancia;

        String numeroLimpo = numeroTelefone.replaceAll("\\D", "");
        if (!numeroLimpo.startsWith("55")) {
            numeroLimpo = "55" + numeroLimpo;
        }

        Map<String, Object> body = Map.of(
                "number", numeroLimpo,
                "text", texto
        );

        try {
            JsonNode resposta = restClient.post()
                    .uri(url)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            System.out.println("Mensagem enviada com sucesso para " + numeroLimpo);
            return resposta == null ? null : texto(resposta.get("key"), "id");
        } catch (Exception e) {
            System.err.println("Erro/Timeout ao enviar WhatsApp para " + nomeInstancia + ": " + e.getMessage());
            return null;
        }
    }

    // ---------------------------------------------------------------- gestão da instância

    public static String nomeInstancia(Long tenantId) {
        return "tenant_" + tenantId;
    }

    /** Estado da conexão: open (conectado), connecting, close ou inexistente. */
    public WhatsAppStatus status(Long tenantId) {
        String instancia = nomeInstancia(tenantId);
        try {
            JsonNode n = chamar(HttpMethod.GET, "/instance/connectionState/" + instancia, null);
            String estado = texto(n.path("instance"), "state");
            if (estado == null) estado = "close";
            return new WhatsAppStatus(instancia, estado, "open".equals(estado));
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return new WhatsAppStatus(instancia, "inexistente", false);
            }
            throw erroDoServico(e);
        }
    }

    /**
     * Prepara a instância (cria se não existir e configura o webhook) e devolve
     * o QR code para parear o número. Se já estiver conectada, não há QR.
     */
    public WhatsAppQr conectar(Long tenantId) {
        String instancia = nomeInstancia(tenantId);
        WhatsAppStatus atual = status(tenantId);
        if (atual.conectado()) {
            return new WhatsAppQr(instancia, "open", true, null, null);
        }

        try {
            if ("inexistente".equals(atual.estado())) {
                chamar(HttpMethod.POST, "/instance/create", Map.of(
                        "instanceName", instancia,
                        "integration", "WHATSAPP-BAILEYS",
                        "qrcode", true,
                        "webhook", Map.of(
                                "url", urlDoWebhook(),
                                "byEvents", false,
                                "base64", false,
                                "events", List.of("MESSAGES_UPSERT"))));
            } else {
                configurarWebhook(instancia);
            }

            JsonNode qr = chamar(HttpMethod.GET, "/instance/connect/" + instancia, null);
            String base64 = texto(qr, "base64");
            String pareamento = texto(qr, "pairingCode");
            // Sem QR ainda: a instância está gerando; o app tenta de novo em instantes.
            String estado = base64 == null ? "connecting" : "qr";
            return new WhatsAppQr(instancia, estado, false, base64, pareamento);
        } catch (RestClientResponseException e) {
            throw erroDoServico(e);
        }
    }

    /** Desconecta o número (logout). A instância continua existindo. */
    public WhatsAppStatus desconectar(Long tenantId) {
        String instancia = nomeInstancia(tenantId);
        try {
            chamar(HttpMethod.DELETE, "/instance/logout/" + instancia, null);
        } catch (RestClientResponseException e) {
            // 404 ou "não conectado": já está desconectado
            if (e.getStatusCode().value() != 404 && e.getStatusCode().value() != 400) {
                throw erroDoServico(e);
            }
        }
        return status(tenantId);
    }

    private void configurarWebhook(String instancia) {
        chamar(HttpMethod.POST, "/webhook/set/" + instancia, Map.of("webhook", Map.of(
                "enabled", true,
                "url", urlDoWebhook(),
                "byEvents", false,
                "base64", false,
                "events", List.of("MESSAGES_UPSERT"))));
    }

    private String urlDoWebhook() {
        if (webhookSecret == null || webhookSecret.isBlank()) return webhookUrl;
        return webhookUrl + (webhookUrl.contains("?") ? "&" : "?") + "token=" + webhookSecret;
    }

    private JsonNode chamar(HttpMethod metodo, String caminho, Object corpo) {
        try {
            RestClient.RequestBodySpec spec = gestao.method(metodo)
                    .uri(apiUrl + caminho)
                    .header("apikey", apiKey);
            if (corpo != null) {
                spec.contentType(MediaType.APPLICATION_JSON).body(corpo);
            }
            return spec.retrieve().body(JsonNode.class);
        } catch (ResourceAccessException e) {
            throw new ServicoIndisponivelException(
                    "Não foi possível falar com o serviço de WhatsApp. Verifique se ele está no ar.");
        }
    }

    private static ServicoIndisponivelException erroDoServico(RestClientResponseException e) {
        return new ServicoIndisponivelException(
                "O serviço de WhatsApp respondeu com erro (" + e.getStatusCode().value() + ").");
    }

    private static String texto(JsonNode no, String campo) {
        if (no == null) return null;
        JsonNode valor = no.get(campo);
        if (valor == null || valor.isNull()) return null;
        String t = valor.asString();
        return t == null || t.isBlank() ? null : t;
    }
}
