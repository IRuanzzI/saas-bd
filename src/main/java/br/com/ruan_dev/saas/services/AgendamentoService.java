package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.models.Agendamento;
import br.com.ruan_dev.saas.models.Cliente;
import br.com.ruan_dev.saas.repositories.AgendamentoRepository;
import br.com.ruan_dev.saas.repositories.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private WhatsAppService whatsAppService;

    public List<Agendamento> listarAgendamentosHoje() {
        return agendamentoRepository.findAgendamentosHoje(LocalDate.now().toString());
    }

    public List<Agendamento> listarAgendamentos() {
        return agendamentoRepository.findAll();
    }

    public Agendamento editarAgendamento(Long id, Agendamento agendamento) {
        Agendamento agendamentoExistente = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado com o ID: " + id));

        agendamentoExistente.setCliente(agendamento.getCliente());
        agendamentoExistente.setUsuario(agendamento.getUsuario());
        agendamentoExistente.setData_hora_inicio(agendamento.getData_hora_inicio());
        agendamentoExistente.setData_hora_final(agendamento.getData_hora_final());
        agendamentoExistente.setStatus(agendamento.getStatus());

        return agendamentoRepository.save(agendamentoExistente);
    }

    public void deletar(Long id) {
        Agendamento agendamentoExistente = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado com o ID: " + id));

        agendamentoRepository.delete(agendamentoExistente);
    }

    @Autowired
    private ClienteRepository clienteRepository; // Adicione a injeção do repositório de clientes

    @Transactional
    public Agendamento criarAgendamento(Agendamento agendamento) {
        if (agendamento == null) {
            throw new RuntimeException("Agendamento não pode ser nulo");
        }

        // 1. Busca os dados completos do cliente no banco (nome, telefone)
        if (agendamento.getCliente() != null && agendamento.getCliente().getId() != null) {
            Cliente clienteCompleto = clienteRepository.findById(agendamento.getCliente().getId())
                    .orElseThrow(() -> new RuntimeException("Cliente não encontrado com ID: " + agendamento.getCliente().getId()));
            agendamento.setCliente(clienteCompleto);
        }

        // 2. Salva o agendamento
        Agendamento agendamentoSalvo = agendamentoRepository.save(agendamento);

        // 3. Dispara a mensagem com o telefone populado
        try {
            LocalDateTime dataInicio = LocalDateTime.parse(agendamentoSalvo.getData_hora_inicio());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
            String dataFormatada = dataInicio.format(formatter);

            String mensagem = "Olá, " + agendamentoSalvo.getCliente().getNome() +
                    ", seu agendamento está marcado para " + dataFormatada;

            String nomeInstancia = "tenant_" + agendamentoSalvo.getTenant().getId();

            whatsAppService.enviarMensagemTexto(
                    nomeInstancia,
                    agendamentoSalvo.getCliente().getTelefone(),
                    mensagem
            );
        } catch (Exception e) {
            System.err.println("Agendamento salvo com sucesso, mas falhou ao enviar WhatsApp: " + e.getMessage());
        }

        return agendamentoSalvo;
    }

    @Transactional
    public void atualizarStatusAgendamento(String telefone, String novoStatus) {
        agendamentoRepository.findProximoAgendamentoPorTelefone(telefone)
                .ifPresent(agendamento -> {


                    agendamento.setStatus(novoStatus);
                    agendamentoRepository.save(agendamento);


                    String nomeInstancia = "tenant_" + agendamento.getTenant().getId();
                    String mensagem = novoStatus.equals("CANCELADO")
                            ? "Tudo certo! Seu agendamento foi cancelado com sucesso. Agradecemos por avisar."
                            : "Perfeito! Seu agendamento está confirmado. Te esperamos!";

                    whatsAppService.enviarMensagemTexto(nomeInstancia, telefone, mensagem);

                    System.out.println("Status do agendamento alterado para " + novoStatus + " via resposta do cliente: " + telefone);
                });
    }
}