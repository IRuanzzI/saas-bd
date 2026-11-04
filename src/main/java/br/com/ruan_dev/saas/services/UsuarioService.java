package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.dto.CreateUsuarioDTO;
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
            throw new RuntimeException("Tenant não identidicado");
        }
        Long tenantId = Long.valueOf(tenantIdStr);
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new RuntimeException("Tenant não encontrado"));

        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()){
            throw new RuntimeException("E-mail já cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(dto.getNome());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setRole(dto.getRole() !=null ? dto.getRole() : "USER");
        usuario.setTenant(tenant);
        usuario.setEmail(dto.getEmail());

        usuarioRepository.save(usuario);
    }

}
