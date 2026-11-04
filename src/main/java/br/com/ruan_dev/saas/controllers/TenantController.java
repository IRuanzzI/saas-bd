package br.com.ruan_dev.saas.controllers;


import br.com.ruan_dev.saas.dto.RegisterTenantDTO;
import br.com.ruan_dev.saas.services.TenantService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {
    @Autowired
    private TenantService tenantService;

    @PostMapping("/register")
    @PreAuthorize("hasAnyAuthority('MASTER')")
    public ResponseEntity<String> registrarTenant(@RequestBody @Valid RegisterTenantDTO dto){
        tenantService.registrarEmpresaEAdmin(dto);
        return ResponseEntity.ok("Empresa e admin gerados");
    }
}
