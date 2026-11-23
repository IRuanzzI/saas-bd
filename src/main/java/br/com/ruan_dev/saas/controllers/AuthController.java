package br.com.ruan_dev.saas.controllers;

import br.com.ruan_dev.saas.dto.AuthResponse;
import br.com.ruan_dev.saas.dto.LoginRequest;
import br.com.ruan_dev.saas.dto.PainelDTOs.AlterarSenha;
import br.com.ruan_dev.saas.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest, HttpServletRequest http) {
        AuthResponse authResponse = authService.login(loginRequest, http.getRemoteAddr());
        return ResponseEntity.ok(authResponse);
    }

    /** Qualquer usuário logado troca a própria senha. */
    @PostMapping("/alterar-senha")
    public ResponseEntity<Void> alterarSenha(@RequestBody @Valid AlterarSenha dto) {
        authService.alterarSenha(dto);
        return ResponseEntity.noContent().build();
    }
}
