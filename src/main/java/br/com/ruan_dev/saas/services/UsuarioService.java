package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.dto.CreateUsuarioDTO;
import br.com.ruan_dev.saas.exceptions.NaoEncontradoException;
import br.com.ruan_dev.saas.exceptions.NegocioException;
import br.com.ruan_dev.saas.models.Tenant;
import br.com.ruan_dev.saas.models.Usuario;
import br.com.ruan_dev.saas.repositories.TenantRepository;
import br.com.ruan_dev.saas.repositories.UsuarioRepository;
import br.com.ruan_dev.saas.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void ciarUsuarioParaTenantAtual(CreateUsuarioDTO dto){
        String tenantIdStr = TenantContext.getCurrentTenant();
        if(tenantIdStr == null){
            throw new NegocioException("Empresa não identificada.");
        }
        Long tenantId = Long.valueOf(tenantIdStr);
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new NaoEncontradoException("Empresa não encontrada."));

        // Checa em todas as empresas (consulta nativa, sem o filtro de empresa).
        if (usuarioRepository.existeEmail(dto.getEmail())){
            throw new NegocioException("E-mail já cadastrado");
        }

        // Um admin só cria USUARIO ou ADMIN: nunca MASTER.
        String role = dto.getRole() != null ? dto.getRole() : "USUARIO";
        if (!role.equals("USUARIO") && !role.equals("ADMIN")) {
            throw new NegocioException("Função inválida. Use USUARIO ou ADMIN.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setRole(role);
        usuario.setTenant(tenant);
        usuario.setEmail(dto.getEmail());

        usuarioRepository.save(usuario);
    }

    /** Usuário logado (identificado pelo e-mail do token JWT). */
    public Usuario obterUsuarioAtual() {
        String email = TenantContext.getCurrentUsuarioEmail();
        if (email == null) {
            throw new NegocioException("Usuário não identificado.");
        }
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
    }

    public String getUsuarioAtual() {
        String usuarioEmail = TenantContext.getCurrentUsuarioEmail();
        if (usuarioEmail != null) {
            return usuarioEmail;
        } else {
            return "Nenhum usuário atual encontrado.";
        }
    }

}
