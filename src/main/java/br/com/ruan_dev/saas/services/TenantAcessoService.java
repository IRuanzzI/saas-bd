package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.models.Tenant;
import br.com.ruan_dev.saas.repositories.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decide se uma empresa pode acessar o sistema (ativa, suspensa ou vencida).
 * Roda a cada requisição autenticada, por isso o resultado fica em cache por
 * alguns segundos. É um serviço à parte (sem PasswordEncoder) para o filtro
 * JWT poder usá-lo sem criar dependência circular.
 */
@Service
public class TenantAcessoService {

    public static final String ATIVA = "ATIVA";
    public static final String SUSPENSA = "SUSPENSA";
    public static final String VENCIDA = "VENCIDA";

    private static final long CACHE_MS = 15_000;

    private record Item(String motivo, long expiraEm) {
    }

    private final Map<Long, Item> cache = new ConcurrentHashMap<>();

    @Autowired
    private TenantRepository tenantRepository;

    public static String situacao(Tenant t) {
        if (!Boolean.TRUE.equals(t.getAtivo())) return SUSPENSA;
        if (t.getVenceEm() != null && t.getVenceEm().isBefore(LocalDate.now())) return VENCIDA;
        return ATIVA;
    }

    /** Mensagem do bloqueio, ou null se a empresa pode acessar. MASTER nunca é bloqueado. */
    public String motivoBloqueio(Long tenantId, String role) {
        if ("MASTER".equals(role)) return null;
        if (tenantId == null) return "Empresa não identificada.";

        Item item = cache.get(tenantId);
        long agora = System.currentTimeMillis();
        if (item != null && item.expiraEm() > agora) return item.motivo();

        // Atenção: não usar Optional.map aqui. Empresa ativa => motivo null, e
        // o map trataria esse null como "vazio" (empresa não encontrada).
        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        String motivo = tenant == null ? "Empresa não encontrada." : motivoDaSituacao(situacao(tenant));

        cache.put(tenantId, new Item(motivo, agora + CACHE_MS));
        return motivo;
    }

    static String motivoDaSituacao(String situacao) {
        return switch (situacao) {
            case SUSPENSA -> "Empresa suspensa. Fale com o suporte.";
            case VENCIDA -> "Assinatura vencida. Fale com o suporte.";
            default -> null;
        };
    }

    /** Chamar quando a situação de uma empresa mudar, para valer na hora. */
    public void invalidar(Long tenantId) {
        cache.remove(tenantId);
    }
}
