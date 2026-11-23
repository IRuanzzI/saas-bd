package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.dto.AuthResponse;
import br.com.ruan_dev.saas.dto.LoginRequest;
import br.com.ruan_dev.saas.dto.PainelDTOs.AlterarSenha;
import br.com.ruan_dev.saas.exceptions.AcessoBloqueadoException;
import br.com.ruan_dev.saas.exceptions.CredenciaisInvalidasException;
import br.com.ruan_dev.saas.exceptions.NaoEncontradoException;
import br.com.ruan_dev.saas.exceptions.NegocioException;
import br.com.ruan_dev.saas.models.Usuario;
import br.com.ruan_dev.saas.repositories.UsuarioRepository;
import br.com.ruan_dev.saas.security.JwtTokenProvider;
import br.com.ruan_dev.saas.security.LoginRateLimiter;
import br.com.ruan_dev.saas.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private LoginRateLimiter limitador;

    @Autowired
    private TenantAcessoService acesso;

    @Autowired
    private AuditoriaService auditoria;

    public AuthResponse login(LoginRequest request, String ip) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String chaveEmail = "e:" + email.toLowerCase();
        String chaveIp = "i:" + ip;

        // 0. Muitas falhas seguidas bloqueiam novas tentativas por um tempo
        limitador.verificar(chaveEmail, LoginRateLimiter.MAX_POR_EMAIL);
        limitador.verificar(chaveIp, LoginRateLimiter.MAX_POR_IP);

        // 1. Busca o usuário e confere a senha
        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null || request.getSenha() == null
                || !passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            limitador.falhou(chaveEmail);
            limitador.falhou(chaveIp);
            throw new CredenciaisInvalidasException();
        }

        // 2. Empresa suspensa ou vencida não entra (o MASTER nunca é bloqueado)
        String bloqueio = acesso.motivoBloqueio(usuario.getTenant().getId(), usuario.getRole());
        if (bloqueio != null) {
            throw new AcessoBloqueadoException(bloqueio);
        }

        limitador.sucesso(chaveEmail);

        String token = jwtTokenProvider.generateToken(usuario);
        // 3. Devolve o payload completo para o Flutter
        return new AuthResponse(
                token,
                usuario.getNome(),
                usuario.getRole(),
                usuario.getTenant().getId(),
                usuario.getTenant().getNome_fantasia(),
                usuario.getId(),
                Boolean.TRUE.equals(usuario.getPrecisaTrocarSenha())
        );
    }

    /** Troca a senha do usuário logado e encerra a obrigação de trocar a provisória. */
    @Transactional
    public void alterarSenha(AlterarSenha dto) {
        String email = TenantContext.getCurrentUsuarioEmail();
        if (email == null) {
            throw new NegocioException("Usuário não identificado.");
        }
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));

        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
            throw new NegocioException("A senha atual está incorreta.");
        }
        if (passwordEncoder.matches(dto.novaSenha(), usuario.getSenha())) {
            throw new NegocioException("A nova senha precisa ser diferente da atual.");
        }

        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuario.setPrecisaTrocarSenha(false);
        usuarioRepository.save(usuario);
        auditoria.registrar(usuario.getTenant().getId(), "SENHA_ALTERADA", usuario.getEmail());
    }
}
