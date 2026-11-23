package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.exceptions.NaoEncontradoException;
import br.com.ruan_dev.saas.exceptions.NegocioException;
import br.com.ruan_dev.saas.dto.PainelDTOs.NotificacaoResumo;
import br.com.ruan_dev.saas.models.Agendamento;
import br.com.ruan_dev.saas.models.Cliente;
import br.com.ruan_dev.saas.repositories.AgendamentoRepository;
import br.com.ruan_dev.saas.repositories.ClienteRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Service
public class AgendamentoService {

    /** Status de todo agendamento novo: o lembrete de 24h pede a confirmação. */
    public static final String STATUS_INICIAL = "PENDENTE";

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private MensagemWhatsappService mensagemWhatsappService;

    @Autowired
    private AuditoriaService auditoria;

    public List<Agendamento> listarAgendamentosHoje() {
        return agendamentoRepository.findAgendamentosHoje(LocalDate.now().toString());
    }

    public List<Agendamento> listarAgendamentos() {
        return agendamentoRepository.findAll();
    }

    /**
     * Atualiza só o que veio na requisição. Empresa e usuário que criou nunca
     * mudam por aqui; o cliente é sempre buscado no banco (da própria empresa).
     */
    public Agendamento editarAgendamento(Long id, Agendamento dados) {
        Agendamento existente = agendamentoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Agendamento não encontrado com o ID: " + id));
        String statusAnterior = existente.getStatus();

        if (dados.getCliente() != null && dados.getCliente().getId() != null) {
            existente.setCliente(buscarCliente(dados.getCliente().getId()));
        }

        boolean horarioMudou = false;
        if (dados.getData_hora_inicio() != null
                && !dados.getData_hora_inicio().equals(existente.getData_hora_inicio())) {
            lerData(dados.getData_hora_inicio());
            existente.setData_hora_inicio(dados.getData_hora_inicio());
            horarioMudou = true;
        }
        if (dados.getData_hora_final() != null) {
            lerData(dados.getData_hora_final());
            existente.setData_hora_final(dados.getData_hora_final());
        }
        if (dados.getStatus() != null && !dados.getStatus().isBlank()) {
            existente.setStatus(dados.getStatus());
        }

        // Mudou o horário: o cliente precisa receber um novo lembrete.
        if (horarioMudou) {
            existente.setLembreteEnviado(false);
        }

        Agendamento salvo = agendamentoRepository.save(existente);

        if (statusAnterior == null
                ? salvo.getStatus() != null
                : !statusAnterior.equals(salvo.getStatus())) {
            notificarAlteracaoStatus(salvo, statusAnterior);
        }

        return salvo;
    }

    public void deletar(Long id) {
        Agendamento agendamentoExistente = agendamentoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Agendamento não encontrado com o ID: " + id));

        agendamentoRepository.delete(agendamentoExistente);
    }

    @Transactional
    public Agendamento criarAgendamento(Agendamento agendamento) {
        if (agendamento == null) {
            throw new NegocioException("Agendamento não pode ser nulo");
        }
        if (agendamento.getCliente() == null || agendamento.getCliente().getId() == null) {
            throw new NegocioException("O cliente é obrigatório");
        }
        if (agendamento.getData_hora_inicio() == null || agendamento.getData_hora_final() == null) {
            throw new NegocioException("Informe o início e o fim do agendamento");
        }
        LocalDateTime inicio = lerData(agendamento.getData_hora_inicio());
        LocalDateTime fim = lerData(agendamento.getData_hora_final());
        if (!fim.isAfter(inicio)) {
            throw new NegocioException("O fim precisa ser depois do início");
        }

        // 1. Busca o cliente completo no banco (nome, telefone), da própria empresa
        agendamento.setCliente(buscarCliente(agendamento.getCliente().getId()));

        // 2. Empresa e usuário vêm do token; o estado inicial é definido aqui
        agendamento.setId(null);
        agendamento.setTenant(tenantService.obterTenantAtual());
        agendamento.setUsuario(usuarioService.obterUsuarioAtual());
        agendamento.setLembreteEnviado(false);
        agendamento.setStatus(STATUS_INICIAL);

        // 3. Salva o agendamento
        Agendamento agendamentoSalvo = agendamentoRepository.save(agendamento);

        // 4. Avisa o cliente no WhatsApp que o agendamento foi criado
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
            String dataFormatada = inicio.format(formatter);

            String mensagem = "Olá, " + agendamentoSalvo.getCliente().getNome() +
                    ", seu agendamento está marcado para " + dataFormatada;

            String nomeInstancia = "tenant_" + agendamentoSalvo.getTenant().getId();

            String idMensagem = whatsAppService.enviarMensagemTexto(
                    nomeInstancia,
                    agendamentoSalvo.getCliente().getTelefone(),
                    mensagem
            );
            // O ID permite reconhecer o cliente se ele responder citando esta mensagem.
            mensagemWhatsappService.registrarEnvio(
                    agendamentoSalvo.getTenant().getId(),
                    idMensagem,
                    agendamentoSalvo.getId(),
                    agendamentoSalvo.getCliente().getId(),
                    MensagemWhatsappService.CRIACAO);
        } catch (Exception e) {
            System.err.println("Agendamento salvo com sucesso, mas falhou ao enviar WhatsApp: " + e.getMessage());
        }

        return agendamentoSalvo;
    }

    /**
     * Aplica a resposta do cliente (CONFIRMADO ou CANCELADO) ao agendamento e
     * devolve a confirmação por WhatsApp, sempre para o TELEFONE cadastrado do
     * cliente (nunca para o LID). Roda em segundo plano para o webhook responder
     * rápido à Evolution.
     */
    @Transactional
    @Async
    public void responderConfirmacao(Long agendamentoId, String novoStatus) {
        try {
            Agendamento agendamento = agendamentoRepository.findById(agendamentoId).orElse(null);
            if (agendamento == null) {
                System.out.println("Resposta ignorada: agendamento " + agendamentoId + " não existe mais.");
                return;
            }

            agendamento.setStatus(novoStatus);
            agendamentoRepository.save(agendamento);

            String nomeCliente = agendamento.getCliente().getNome();
            Long tenantId = agendamento.getTenant().getId();
            String mensagemResposta = "";

            if (novoStatus.equals("CONFIRMADO")) {
                mensagemResposta = "✅ Olá " + nomeCliente + ", seu agendamento foi *confirmado* com sucesso! Te esperamos no horário marcado.";
            } else if (novoStatus.equals("CANCELADO")) {
                mensagemResposta = "❌ Olá " + nomeCliente + ", seu agendamento foi *cancelado*. Se precisar marcar um novo horário no futuro, é só nos chamar aqui!";
            }

            auditoria.registrar(tenantId, "cliente via WhatsApp", "AGENDAMENTO_" + novoStatus,
                    nomeCliente + " · " + agendamento.getData_hora_inicio());

            whatsAppService.enviarMensagemTexto(
                    WhatsAppService.nomeInstancia(tenantId),
                    agendamento.getCliente().getTelefone(),
                    mensagemResposta);
            System.out.println("Feedback de " + novoStatus + " enviado para o cliente " + nomeCliente);
        } catch (Exception e) {
            // Em @Async a exceção se perderia em silêncio: registra para dar para diagnosticar.
            System.err.println("Falha ao aplicar a resposta do cliente ao agendamento " + agendamentoId + ": " + e.getMessage());
        }
    }

    public List<Agendamento> listarAgendamentosPorCliente(Long clienteId) {
        return agendamentoRepository.findByClienteId(clienteId);
    }

    public NotificacaoResumo resumoNotificacoes() {
        List<Agendamento> hoje = listarAgendamentosHoje();

        long pendentes = contarStatus(hoje, "PENDENTE");
        long confirmados = contarStatus(hoje, "CONFIRMADO");
        long cancelados = contarStatus(hoje, "CANCELADO");
        long finalizados = contarStatus(hoje, "FINALIZADO");
        long lembretesEnviados = hoje.stream()
                .filter(Agendamento::getLembreteEnviado)
                .count();

        long lembretesPendentes = hoje.stream()
                .filter(a -> "PENDENTE".equals(a.getStatus()))
                .filter(a -> !Boolean.TRUE.equals(a.getLembreteEnviado()))
                .count();

        return new NotificacaoResumo(
                hoje.size(),
                pendentes,
                confirmados,
                cancelados,
                finalizados,
                lembretesEnviados,
                lembretesPendentes);
    }

    private long contarStatus(List<Agendamento> agendamentos, String status) {
        return agendamentos.stream()
                .filter(a -> status.equalsIgnoreCase(a.getStatus()))
                .count();
    }

    private void notificarAlteracaoStatus(Agendamento agendamento, String statusAnterior) {
        try {
            String novoStatus = agendamento.getStatus();
            String textoStatus;
            if ("CONFIRMADO".equalsIgnoreCase(novoStatus)) {
                textoStatus = "confirmado";
            } else if ("CANCELADO".equalsIgnoreCase(novoStatus)) {
                textoStatus = "cancelado";
            } else if ("FINALIZADO".equalsIgnoreCase(novoStatus)) {
                textoStatus = "finalizado";
            } else {
                textoStatus = novoStatus.toLowerCase();
            }

            whatsAppService.enviarMensagemTexto(
                    WhatsAppService.nomeInstancia(agendamento.getTenant().getId()),
                    agendamento.getCliente().getTelefone(),
                    "Olá, " + agendamento.getCliente().getNome()
                            + ". Seu agendamento foi alterado de " + statusAnterior
                            + " para " + textoStatus + ".");

            auditoria.registrar(
                    agendamento.getTenant().getId(),
                    usuarioService.obterUsuarioAtual().getNome(),
                    "AGENDAMENTO_STATUS_ALTERADO",
                    agendamento.getCliente().getNome() + " · "
                            + statusAnterior + " -> " + novoStatus);
        } catch (Exception e) {
            System.err.println("Falha ao notificar alteração de status do agendamento "
                    + agendamento.getId() + ": " + e.getMessage());
        }
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Cliente não encontrado com ID: " + id));
    }

    private LocalDateTime lerData(String texto) {
        try {
            return LocalDateTime.parse(texto);
        } catch (DateTimeParseException e) {
            throw new NegocioException("Data inválida. Use o formato yyyy-MM-ddTHH:mm:ss");
        }
    }

}
