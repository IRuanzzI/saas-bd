package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.dto.AuthResponse;
import br.com.ruan_dev.saas.dto.LoginRequest;
import br.com.ruan_dev.saas.models.Usuario;
import br.com.ruan_dev.saas.repositories.UsuarioRepository;
import br.com.ruan_dev.saas.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public AuthResponse login(LoginRequest request) {
        // 1. Busca o usuário
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            throw new RuntimeException("Credenciais inválidas");
        }


        String token = jwtTokenProvider.generateToken(usuario);

        // 4. Devolve o payload completo para o Flutter
        return new AuthResponse(
                token,
                usuario.getNome(),
                usuario.getRole(),
                usuario.getTenant().getId()
        );
    }
}