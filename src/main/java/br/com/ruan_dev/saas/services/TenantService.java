package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.dto.RegisterTenantDTO;
import br.com.ruan_dev.saas.models.Tenant;
import br.com.ruan_dev.saas.models.Usuario;
import br.com.ruan_dev.saas.repositories.TenantRepository;
import br.com.ruan_dev.saas.repositories.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public void registrarEmpresaEAdmin(RegisterTenantDTO dto){
        if (usuarioRepository.findByEmail(dto.getAdminEmail()).isPresent()){
            throw new RuntimeException("E-mail já cadastrado no sistema.");
        }



        Tenant tenant = new Tenant();
        tenant.setNome_fantasia(dto.getNomeFantasia());
        tenant.setDocumento(dto.getDocumento());
        tenant.setAtivo(true);
        tenant = tenantRepository.save(tenant);

        Usuario admin = new Usuario();
        admin.setNome(dto.getAdminNome());
        admin.setSenha(passwordEncoder.encode(dto.getAdminSenha()));
        admin.setEmail(dto.getAdminEmail());
        admin.setRole("ADMIN");
        admin.setTenant(tenant);

        usuarioRepository.save(admin);
    }



}
