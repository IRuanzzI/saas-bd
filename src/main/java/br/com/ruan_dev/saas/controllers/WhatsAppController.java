package br.com.ruan_dev.saas.controllers;

import br.com.ruan_dev.saas.dto.PainelDTOs.WhatsAppQr;
import br.com.ruan_dev.saas.dto.PainelDTOs.WhatsAppStatus;
import br.com.ruan_dev.saas.exceptions.AcessoBloqueadoException;
import br.com.ruan_dev.saas.exceptions.NaoEncontradoException;
import br.com.ruan_dev.saas.exceptions.NegocioException;
import br.com.ruan_dev.saas.repositories.TenantRepository;
import br.com.ruan_dev.saas.security.TenantContext;
import br.com.ruan_dev.saas.services.AuditoriaService;
import br.com.ruan_dev.saas.services.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * WhatsApp de uma empresa: status, QR code para parear o número e desconexão.
 * O ADMIN gerencia a própria empresa; o MASTER pode informar ?tenantId= para
 * gerenciar a de qualquer cliente (painel).
 */
@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsAppController {

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AuditoriaService auditoria;

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('MASTER', 'ADMIN')")
    public WhatsAppStatus status(@RequestParam(required = false) Long tenantId) {
        return whatsAppService.status(alvo(tenantId));
    }

    @PostMapping("/conectar")
    @PreAuthorize("hasAnyAuthority('MASTER', 'ADMIN')")
    public WhatsAppQr conectar(@RequestParam(required = false) Long tenantId) {
        Long alvo = alvo(tenantId);
        WhatsAppQr qr = whatsAppService.conectar(alvo);
        if ("qr".equals(qr.estado())) {
            auditoria.registrar(alvo, "WHATSAPP_QR_GERADO", "Instância " + qr.instancia());
        }
        return qr;
    }

    @DeleteMapping("/desconectar")
    @PreAuthorize("hasAnyAuthority('MASTER', 'ADMIN')")
    public WhatsAppStatus desconectar(@RequestParam(required = false) Long tenantId) {
        Long alvo = alvo(tenantId);
        WhatsAppStatus status = whatsAppService.desconectar(alvo);
        auditoria.registrar(alvo, "WHATSAPP_DESCONECTADO", "Instância " + status.instancia());
        return status;
    }

    /** Empresa alvo: a do usuário, ou (só MASTER) a informada em tenantId. */
    private Long alvo(Long tenantIdParam) {
        String proprioTexto = TenantContext.getCurrentTenant();
        if (proprioTexto == null) {
            throw new NegocioException("Empresa não identificada.");
        }
        Long proprio = Long.valueOf(proprioTexto);
        if (tenantIdParam == null || tenantIdParam.equals(proprio)) {
            return proprio;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean master = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("MASTER"));
        if (!master) {
            throw new AcessoBloqueadoException("Você só pode gerenciar o WhatsApp da sua empresa.");
        }
        if (!tenantRepository.existsById(tenantIdParam)) {
            throw new NaoEncontradoException("Empresa não encontrada.");
        }
        return tenantIdParam;
    }
}
