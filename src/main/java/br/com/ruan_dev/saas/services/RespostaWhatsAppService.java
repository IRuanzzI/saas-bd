package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.models.Agendamento;
import br.com.ruan_dev.saas.models.MensagemWhatsapp;
import br.com.ruan_dev.saas.repositories.AgendamentoRepository;
import br.com.ruan_dev.saas.repositories.MensagemWhatsappRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decide de quem é uma resposta ("S" / "N") recebida pelo WhatsApp e atualiza o
 * agendamento certo.
 *
 * O WhatsApp pode identificar o remetente de duas formas:
 * - pelo telefone (…@s.whatsapp.net): casamos com o telefone do cliente;
 * - por um código interno, o LID (…@lid), sem telefone nenhum. Nesse caso
 *   usamos, em ordem: (1) o LID que já aprendemos desse cliente; (2) a mensagem
 *   que ele respondeu (citada), cujo ID guardamos ao enviar; (3) o nome do
 *   perfil (pushName), só quando exatamente UM cliente aguardando resposta
 *   combina. Se houver dúvida, não age: melhor ignorar do que confirmar o
 *   agendamento de outra pessoa.
 */
@Service
public class RespostaWhatsAppService {

    public static final String CONFIRMADO = "CONFIRMADO";
    public static final String CANCELADO = "CANCELADO";

    private static final Pattern INSTANCIA = Pattern.compile("^tenant_(\\d+)$");
    private static final Set<String> CONFIRMA = Set.of("S", "SIM", "CONFIRMO", "CONFIRMAR", "CONFIRMADO");
    private static final Set<String> CANCELA = Set.of("N", "NAO", "CANCELAR", "CANCELO", "CANCELADO");

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private MensagemWhatsappRepository mensagemRepository;

    @Autowired
    private AgendamentoService agendamentoService;

    /**
     * Processa uma mensagem recebida. Devolve um texto curto dizendo o que
     * aconteceu (vai para o log, para dar para diagnosticar depois).
     */
    @Transactional
    public String processar(String instancia, String remoteJid, String pushName, String texto, String citadaId) {
        Long tenantId = tenantDaInstancia(instancia);
        if (tenantId == null) return "ignorado: instância desconhecida (" + instancia + ")";

        String acao = interpretar(texto);
        if (acao == null) return "ignorado: o texto não é uma resposta de confirmação";

        String desde = LocalDateTime.now().minusHours(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        List<Agendamento> candidatos = agendamentoRepository.findAguardandoResposta(tenantId, desde).stream()
                .filter(a -> aceita(a.getStatus(), acao))
                .toList();
        if (candidatos.isEmpty()) return "ignorado: nenhum agendamento aguardando resposta nesta empresa";

        String jid = remoteJid == null ? "" : remoteJid;
        Agendamento escolhido;
        String lidParaAprender = null;

        if (jid.endsWith("@lid")) {
            escolhido = primeiro(candidatos, a -> jid.equals(a.getCliente().getWhatsappLid()));

            if (escolhido == null && citadaId != null && !citadaId.isBlank()) {
                Optional<MensagemWhatsapp> citada = mensagemRepository.findFirstByTenantIdAndMessageId(tenantId, citadaId);
                if (citada.isPresent() && citada.get().getClienteId() != null) {
                    Long clienteId = citada.get().getClienteId();
                    escolhido = primeiro(candidatos, a -> clienteId.equals(a.getCliente().getId()));
                    if (escolhido != null) lidParaAprender = jid;
                }
            }

            if (escolhido == null) {
                List<Agendamento> combinam = candidatos.stream()
                        .filter(a -> nomeCombina(a.getCliente().getNome(), pushName))
                        .toList();
                long clientes = combinam.stream().map(a -> a.getCliente().getId()).distinct().count();
                if (clientes == 0) {
                    return "ignorado: remetente " + jid + " (" + pushName + ") não é de nenhum cliente aguardando resposta";
                }
                if (clientes > 1) {
                    return "ignorado: mais de um cliente combina com \"" + pushName + "\"; peça para responder citando a mensagem";
                }
                escolhido = combinam.get(0);
                lidParaAprender = jid;
            }
        } else {
            String chave = chaveTelefone(jid.split("@")[0]);
            escolhido = primeiro(candidatos, a -> chave.equals(chaveTelefone(a.getCliente().getTelefone())));
            if (escolhido == null) return "ignorado: nenhum cliente com o telefone " + chave;
        }

        if (lidParaAprender != null && !lidParaAprender.equals(escolhido.getCliente().getWhatsappLid())) {
            // Entidade gerenciada: salva ao fim da transação.
            escolhido.getCliente().setWhatsappLid(lidParaAprender);
        }
        agendamentoService.responderConfirmacao(escolhido.getId(), acao);
        return "ok: agendamento " + escolhido.getId() + " (" + escolhido.getCliente().getNome() + ") -> " + acao;
    }

    // ---------------------------------------------------------------- regras puras (testáveis)

    /** "S"/"SIM"... => CONFIRMADO; "N"/"NÃO"/"CANCELAR"... => CANCELADO; senão null. */
    public static String interpretar(String texto) {
        if (texto == null) return null;
        String limpo = semAcento(texto).toUpperCase().replaceAll("[^A-Z ]", " ").trim().replaceAll("\\s+", " ");
        if (limpo.isEmpty()) return null;
        if (CONFIRMA.contains(limpo)) return CONFIRMADO;
        if (CANCELA.contains(limpo)) return CANCELADO;
        // "Sim, confirmo!" / "Não posso" : primeira palavra decide, em mensagens curtas
        String[] palavras = limpo.split(" ");
        if (palavras.length <= 3) {
            if (palavras[0].equals("SIM")) return CONFIRMADO;
            if (palavras[0].equals("NAO")) return CANCELADO;
        }
        return null;
    }

    /** Quem pode receber a resposta: "S" só vale para pendente; "N" vale para pendente ou confirmado. */
    static boolean aceita(String statusAtual, String acao) {
        if (CONFIRMADO.equals(acao)) return "PENDENTE".equals(statusAtual);
        return "PENDENTE".equals(statusAtual) || CONFIRMADO.equals(statusAtual);
    }

    /** Empresa da instância "tenant_<id>", ou null. */
    public static Long tenantDaInstancia(String instancia) {
        if (instancia == null) return null;
        Matcher m = INSTANCIA.matcher(instancia.trim());
        return m.matches() ? Long.valueOf(m.group(1)) : null;
    }

    /**
     * Chave para comparar telefones brasileiros: DDD + últimos 8 dígitos. Assim
     * "5538999884360" e "553899884360" (com e sem o nono dígito) são o mesmo número.
     */
    public static String chaveTelefone(String telefone) {
        String d = telefone == null ? "" : telefone.replaceAll("\\D", "");
        if (d.startsWith("55") && d.length() >= 12) d = d.substring(2);
        if (d.length() < 10) return d;
        return d.substring(0, 2) + d.substring(d.length() - 8);
    }

    /** O nome do perfil do WhatsApp (ex.: "Maria Diniz💕") combina com o do cliente (ex.: "Maria")? */
    public static boolean nomeCombina(String nomeCliente, String pushName) {
        Set<String> a = palavras(nomeCliente);
        Set<String> b = palavras(pushName);
        a.retainAll(b);
        return !a.isEmpty();
    }

    private static Set<String> palavras(String texto) {
        Set<String> r = new LinkedHashSet<>();
        if (texto == null) return r;
        String limpo = semAcento(texto).toLowerCase().replaceAll("[^a-z ]", " ");
        for (String p : limpo.split("\\s+")) {
            if (p.length() >= 3) r.add(p);
        }
        return r;
    }

    private static String semAcento(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static Agendamento primeiro(List<Agendamento> lista, Predicate<Agendamento> regra) {
        return lista.stream().filter(regra).findFirst().orElse(null);
    }
}
