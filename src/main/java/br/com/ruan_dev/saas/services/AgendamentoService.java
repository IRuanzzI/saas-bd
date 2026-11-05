package br.com.ruan_dev.saas.services;


import br.com.ruan_dev.saas.models.Agendamento;
import br.com.ruan_dev.saas.repositories.AgendamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    public List<Agendamento> listarAgendamentosHoje() {
        return agendamentoRepository.findAgendamentosHoje();
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

    public Void deletar(Long id) {
        Agendamento agendamentoExistente = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado com o ID: " + id));

        agendamentoRepository.delete(agendamentoExistente);
        return null;
    }

    public Agendamento criarAgendamento(Agendamento agendamento) {
        if(agendamento == null) {
            throw new RuntimeException("Agendamento não pode ser nulo");
        }
        return agendamentoRepository.save(agendamento);
    }

}
