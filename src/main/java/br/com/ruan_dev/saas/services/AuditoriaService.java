package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.dto.PainelDTOs.Auditoria;
import br.com.ruan_dev.saas.models.AuditoriaLog;
import br.com.ruan_dev.saas.repositories.AuditoriaLogRepository;
import br.com.ruan_dev.saas.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaLogRepository repository;

    /** Registra uma ação; o ator é o usuário do token (ou "sistema"). */
    public void registrar(Long tenantAlvoId, String acao, String detalhe) {
        registrar(tenantAlvoId, TenantContext.getCurrentUsuarioEmail(), acao, detalhe);
    }

    public void registrar(Long tenantAlvoId, String ator, String acao, String detalhe) {
        AuditoriaLog log = new AuditoriaLog();
        log.setTenantAlvoId(tenantAlvoId);
        log.setAtor(ator != null ? ator : "sistema");
        log.setAcao(acao);
        log.setDetalhe(detalhe != null && detalhe.length() > 500 ? detalhe.substring(0, 500) : detalhe);
        log.setCriadoEm(LocalDateTime.now());
        repository.save(log);
    }

    public List<Auditoria> recentes(Long tenantAlvoId) {
        return repository.findTop30ByTenantAlvoIdOrderByIdDesc(tenantAlvoId).stream()
                .map(l -> new Auditoria(l.getId(), l.getCriadoEm().toString(),
                        l.getAtor(), l.getAcao(), l.getDetalhe()))
                .toList();
    }
}
