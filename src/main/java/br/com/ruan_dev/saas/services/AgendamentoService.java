package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.models.Agendamento;
import br.com.ruan_dev.saas.repositories.AgendamentoRepository;
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

    @Transactional
    public Agendamento criarAgendamento(Agendamento agendamento) {
        if (agendamento == null) {
            throw new RuntimeException("Agendamento não pode ser nulo");
        }

        // 1. Salva primeiro no banco de dados
        Agendamento agendamentoSalvo = agendamentoRepository.save(agendamento);


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

        // 3. Retorna a entidade salva
        return agendamentoSalvo;
    }
}