package br.com.ruan_dev.saas.controllers;


import br.com.ruan_dev.saas.dto.PainelDTOs.Ativo;
import br.com.ruan_dev.saas.dto.PainelDTOs.Auditoria;
import br.com.ruan_dev.saas.dto.PainelDTOs.SenhaTemporaria;
import br.com.ruan_dev.saas.dto.PainelDTOs.TenantAtualizar;
import br.com.ruan_dev.saas.dto.PainelDTOs.TenantResumo;
import br.com.ruan_dev.saas.dto.RegisterTenantDTO;
import br.com.ruan_dev.saas.services.AuditoriaService;
import br.com.ruan_dev.saas.services.TenantService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {
    @Autowired
    private TenantService tenantService;

    @Autowired
    private AuditoriaService auditoriaService;

    // ---- painel do MASTER

    @GetMapping
    @PreAuthorize("hasAuthority('MASTER')")
    public List<TenantResumo> listar() {
        return tenantService.listar();
    }

    @PostMapping("/register")
    @PreAuthorize("hasAnyAuthority('MASTER')")
    public ResponseEntity<TenantResumo> registrarTenant(@RequestBody @Valid RegisterTenantDTO dto){
        return ResponseEntity.ok(tenantService.registrarEmpresaEAdmin(dto));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER')")
    public TenantResumo detalhar(@PathVariable Long id) {
        return tenantService.detalhar(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MASTER')")
    public TenantResumo atualizar(@PathVariable Long id, @RequestBody @Valid TenantAtualizar dto) {
        return tenantService.atualizar(id, dto);
    }

    @PutMapping("/{id}/ativo")
    @PreAuthorize("hasAuthority('MASTER')")
    public TenantResumo definirAtivo(@PathVariable Long id, @RequestBody @Valid Ativo dto) {
        return tenantService.definirAtivo(id, dto.ativo());
    }

    @PostMapping("/{id}/redefinir-senha-admin")
    @PreAuthorize("hasAuthority('MASTER')")
    public SenhaTemporaria redefinirSenhaAdmin(@PathVariable Long id) {
        return tenantService.redefinirSenhaAdmin(id);
    }

    @GetMapping("/{id}/auditoria")
    @PreAuthorize("hasAuthority('MASTER')")
    public List<Auditoria> auditoria(@PathVariable Long id) {
        return auditoriaService.recentes(id);
    }

    // ---- qualquer usuário logado

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('MASTER', 'ADMIN', 'USUARIO')")
    public ResponseEntity<String> getTenantAtual() {
        String tenantAtual = tenantService.getTenantAtual();
        return ResponseEntity.ok(tenantAtual);
    }
}
