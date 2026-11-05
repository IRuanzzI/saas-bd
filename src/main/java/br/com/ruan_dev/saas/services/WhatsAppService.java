package br.com.ruan_dev.saas.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class WhatsAppService {

    @Value("${whatsapp.api.url:http://localhost:8081}")
    private String apiUrl;

    @Value("${whatsapp.api.token:ruan_saas_secret_key_2026}")
    private String apiKey;

    private final RestClient restClient;

    public WhatsAppService() {
        // Configura timeouts de 5 segundos para conexão e leitura
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public void enviarMensagemTexto(String nomeInstancia, String numeroTelefone, String texto) {
        String url = apiUrl + "/message/sendText/" + nomeInstancia;

        String numeroLimpo = numeroTelefone.replaceAll("\\D", "");
        if (!numeroLimpo.startsWith("55")) {
            numeroLimpo = "55" + numeroLimpo;
        }

        Map<String, Object> body = Map.of(
                "number", numeroLimpo,
                "textMessage", Map.of("text", texto)
        );

        try {
            restClient.post()
                    .uri(url)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            System.out.println("Mensagem enviada com sucesso para " + numeroLimpo);
        } catch (Exception e) {
            System.err.println("Erro/Timeout ao enviar WhatsApp para " + nomeInstancia + ": " + e.getMessage());
        }
    }
}