package br.com.ruan_dev.saas.controllers;


import br.com.ruan_dev.saas.models.Agendamento;
import br.com.ruan_dev.saas.services.AgendamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agendamentos")
public class AgendamentoController {

    @Autowired
    private AgendamentoService agendamentoService;

    @GetMapping("/hoje")
    @PreAuthorize("hasAnyAuthority('MASTER', 'ADMIN', 'USUARIO')")
    public List<Agendamento> listarAgendamentosHoje() {
        return agendamentoService.listarAgendamentosHoje();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MASTER', 'ADMIN', 'USUARIO')")
    public List<Agendamento> listarTodosAgendamentos() {
        return agendamentoService.listarAgendamentos();
    }

    @PutMapping("/edit/{id}")
    @PreAuthorize("hasAnyAuthority('MASTER', 'ADMIN', 'USUARIO')")
    public Agendamento editarAgendamento(@RequestBody Agendamento agendamento, @PathVariable Long id) {
        Agendamento agendamento1 = agendamentoService.editarAgendamento(id, agendamento);
        return agendamento1;
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyAuthority('MASTER', 'ADMIN', 'USUARIO')")
    public ResponseEntity<Void> deletarAgendamento(@PathVariable Long id) {
         agendamentoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('MASTER', 'ADMIN', 'USUARIO')")
    public Agendamento criarAgendamento(@RequestBody Agendamento agendamento) {
        return agendamentoService.criarAgendamento(agendamento);
    }

}
