package br.com.ruan_dev.saas.controllers;

import br.com.ruan_dev.saas.dto.CreateUsuarioDTO;
import br.com.ruan_dev.saas.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.authorization.AuthorityReactiveAuthorizationManager.hasAnyAuthority;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<String> criarUsuario(@RequestBody @Valid CreateUsuarioDTO dto){
        usuarioService.ciarUsuarioParaTenantAtual(dto);
        return ResponseEntity.ok("Usuario criado com sucesso para esta empresa");
    }
}
