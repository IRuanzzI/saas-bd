package br.com.ruan_dev.saas.controllers;

import br.com.ruan_dev.saas.dto.PainelDTOs.NotificacaoResumo;
import br.com.ruan_dev.saas.services.AgendamentoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notificacoes")
public class NotificacoesController {

    @Autowired
    private AgendamentoService agendamentoService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MASTER', 'ADMIN', 'USUARIO')")
    public NotificacaoResumo resumo() {
        return agendamentoService.resumoNotificacoes();
    }

}
