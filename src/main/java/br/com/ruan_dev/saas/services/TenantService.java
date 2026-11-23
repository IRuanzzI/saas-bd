package br.com.ruan_dev.saas.services;

import br.com.ruan_dev.saas.dto.PainelDTOs.SenhaTemporaria;
import br.com.ruan_dev.saas.dto.PainelDTOs.TenantAtualizar;
import br.com.ruan_dev.saas.dto.PainelDTOs.TenantResumo;
import br.com.ruan_dev.saas.dto.RegisterTenantDTO;
import br.com.ruan_dev.saas.exceptions.NaoEncontradoException;
import br.com.ruan_dev.saas.exceptions.NegocioException;
import br.com.ruan_dev.saas.models.Tenant;
import br.com.ruan_dev.saas.models.Usuario;
import br.com.ruan_dev.saas.repositories.TenantRepository;
import br.com.ruan_dev.saas.repositories.UsuarioRepository;
import br.com.ruan_dev.saas.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class TenantService {

    // Sem caracteres que se confundem (0/O, 1/l/I).
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private final SecureRandom random = new SecureRandom();

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TenantAcessoService acesso;

    @Autowired
    private AuditoriaService auditoria;

    // ---------------------------------------------------------------- painel do MASTER

    @Transactional
    public TenantResumo registrarEmpresaEAdmin(RegisterTenantDTO dto) {
        String documento = soDigitos(dto.getDocumento());
        if (documento.length() != 11 && documento.length() != 14) {
            throw new NegocioException("Informe um CPF (11 dígitos) ou um CNPJ (14 dígitos).");
        }
        if (tenantRepository.existsByDocumento(documento)) {
            throw new NegocioException("Já existe uma empresa com este CPF/CNPJ.");
        }
        String email = dto.getAdminEmail().trim();
        // Checagem em todas as empresas (a consulta é nativa, sem o filtro de empresa).
        if (usuarioRepository.existeEmail(email)) {
            throw new NegocioException("E-mail já cadastrado no sistema.");
        }
        LocalDate venceEm = lerData(dto.getVenceEm());

        Tenant tenant = new Tenant();
        tenant.setNome_fantasia(dto.getNomeFantasia().trim());
        tenant.setDocumento(documento);
        tenant.setAtivo(true);
        tenant.setPlano(textoOuNulo(dto.getPlano()) != null ? dto.getPlano().trim() : "TESTE");
        tenant.setVenceEm(venceEm);
        tenant = tenantRepository.save(tenant);

        Usuario admin = new Usuario();
        admin.setNome(dto.getAdminNome().trim());
        admin.setSenha(passwordEncoder.encode(dto.getAdminSenha()));
        admin.setEmail(email);
        admin.setRole("ADMIN");
        admin.setTenant(tenant);
        admin.setPrecisaTrocarSenha(true);
        usuarioRepository.save(admin);

        auditoria.registrar(tenant.getId(), "EMPRESA_CRIADA",
                "Empresa \"" + tenant.getNome_fantasia() + "\" criada com o admin " + email);
        return resumo(tenant);
    }

    public List<TenantResumo> listar() {
        return tenantRepository.findAll(Sort.by("id")).stream().map(this::resumo).toList();
    }

    public TenantResumo detalhar(Long id) {
        return resumo(buscar(id));
    }

    @Transactional
    public TenantResumo atualizar(Long id, TenantAtualizar dto) {
        Tenant tenant = buscar(id);
        String antes = tenant.getNome_fantasia() + " / " + tenant.getPlano() + " / " + tenant.getVenceEm();

        tenant.setNome_fantasia(dto.nomeFantasia().trim());
        tenant.setPlano(textoOuNulo(dto.plano()));
        tenant.setVenceEm(lerData(dto.venceEm()));
        tenantRepository.save(tenant);
        acesso.invalidar(id);

        String depois = tenant.getNome_fantasia() + " / " + tenant.getPlano() + " / " + tenant.getVenceEm();
        auditoria.registrar(id, "EMPRESA_ATUALIZADA", antes + " -> " + depois);
        return resumo(tenant);
    }

    @Transactional
    public TenantResumo definirAtivo(Long id, boolean ativo) {
        Tenant tenant = buscar(id);
        String proprio = TenantContext.getCurrentTenant();
        if (!ativo && proprio != null && proprio.equals(String.valueOf(id))) {
            throw new NegocioException("Você não pode suspender a sua própria empresa.");
        }
        tenant.setAtivo(ativo);
        tenantRepository.save(tenant);
        acesso.invalidar(id);
        auditoria.registrar(id, ativo ? "EMPRESA_REATIVADA" : "EMPRESA_SUSPENSA",
                "Empresa \"" + tenant.getNome_fantasia() + "\"");
        return resumo(tenant);
    }

    /**
     * Gera uma senha provisória para o admin da empresa. Ela só é devolvida
     * aqui (nunca fica em log nem na auditoria) e o admin precisa trocá-la no
     * próximo acesso.
     */
    @Transactional
    public SenhaTemporaria redefinirSenhaAdmin(Long id) {
        buscar(id);
        Usuario admin = usuarioRepository.findPrimeiroAdmin(id)
                .orElseThrow(() -> new NaoEncontradoException("Esta empresa não tem um administrador."));

        String temporaria = gerarSenhaTemporaria();
        admin.setSenha(passwordEncoder.encode(temporaria));
        admin.setPrecisaTrocarSenha(true);
        usuarioRepository.save(admin);

        auditoria.registrar(id, "SENHA_ADMIN_REDEFINIDA", "Admin " + admin.getEmail());
        return new SenhaTemporaria(admin.getEmail(), temporaria);
    }

    // ---------------------------------------------------------------- empresa do usuário logado

    /** Empresa do usuário logado (vem do token JWT, nunca do corpo da requisição). */
    public Tenant obterTenantAtual() {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new NegocioException("Empresa não identificada.");
        }
        return tenantRepository.findById(Long.valueOf(tenantId))
                .orElseThrow(() -> new NaoEncontradoException("Empresa não encontrada."));
    }

    public String getTenantAtual() {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return "Nenhum tenant atual encontrado.";
        }

        return tenantRepository.findById(Long.valueOf(tenantId))
                .map(Tenant::getNome_fantasia)
                .orElse("Nenhum tenant atual encontrado.");
    }

    // ---------------------------------------------------------------- auxiliares

    private Tenant buscar(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Empresa não encontrada."));
    }

    private TenantResumo resumo(Tenant t) {
        return new TenantResumo(
                t.getId(),
                t.getNome_fantasia(),
                t.getDocumento(),
                Boolean.TRUE.equals(t.getAtivo()),
                t.getPlano(),
                t.getVenceEm() == null ? null : t.getVenceEm().toString(),
                TenantAcessoService.situacao(t),
                tenantRepository.contarClientes(t.getId()),
                tenantRepository.contarAgendamentos(t.getId()),
                tenantRepository.nomeDoAdmin(t.getId()).orElse(null),
                tenantRepository.emailDoAdmin(t.getId()).orElse(null));
    }

    private String gerarSenhaTemporaria() {
        StringBuilder senha = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            senha.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        return senha.toString();
    }

    private static String soDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static LocalDate lerData(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            throw new NegocioException("Data inválida. Use o formato yyyy-MM-dd.");
        }
    }
}
