package br.com.ruan_dev.saas.security;

import br.com.ruan_dev.saas.exceptions.LimiteExcedidoException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limita tentativas de login com falha, por e-mail e por IP, em memória.
 * Depois de [max] falhas dentro da janela, bloqueia até a janela passar.
 * (Com mais de uma instância da API, trocar por um contador compartilhado.)
 */
@Component
public class LoginRateLimiter {

    public static final int MAX_POR_EMAIL = 5;
    public static final int MAX_POR_IP = 30;
    private static final Duration JANELA = Duration.ofMinutes(15);

    private record Tentativas(int falhas, Instant inicio) {
        boolean expirou() {
            return Instant.now().isAfter(inicio.plus(JANELA));
        }
    }

    private final Map<String, Tentativas> mapa = new ConcurrentHashMap<>();

    /** Lança 429 se a chave já estourou o limite. */
    public void verificar(String chave, int max) {
        Tentativas t = mapa.get(chave);
        if (t == null) return;
        if (t.expirou()) {
            mapa.remove(chave);
            return;
        }
        if (t.falhas() >= max) {
            throw new LimiteExcedidoException(
                    "Muitas tentativas de login. Tente novamente em alguns minutos.");
        }
    }

    public void falhou(String chave) {
        if (mapa.size() > 10_000) {
            mapa.entrySet().removeIf(e -> e.getValue().expirou());
        }
        mapa.merge(chave, new Tentativas(1, Instant.now()),
                (atual, nova) -> atual.expirou() ? nova : new Tentativas(atual.falhas() + 1, atual.inicio()));
    }

    public void sucesso(String chave) {
        mapa.remove(chave);
    }
}
