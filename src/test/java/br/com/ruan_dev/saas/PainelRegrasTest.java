package br.com.ruan_dev.saas;

import br.com.ruan_dev.saas.dto.AuthResponse;
import br.com.ruan_dev.saas.exceptions.GlobalExceptionHandler;
import br.com.ruan_dev.saas.exceptions.LimiteExcedidoException;
import br.com.ruan_dev.saas.models.Tenant;
import br.com.ruan_dev.saas.models.Usuario;
import br.com.ruan_dev.saas.repositories.TenantRepository;
import br.com.ruan_dev.saas.security.LoginRateLimiter;
import br.com.ruan_dev.saas.services.TenantAcessoService;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PainelRegrasTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private static Tenant tenant(boolean ativo, LocalDate venceEm) {
        Tenant t = new Tenant();
        t.setAtivo(ativo);
        t.setVenceEm(venceEm);
        return t;
    }

    @Test
    void situacaoDaEmpresa() {
        assertThat(TenantAcessoService.situacao(tenant(true, null))).isEqualTo("ATIVA");
        assertThat(TenantAcessoService.situacao(tenant(true, LocalDate.now()))).isEqualTo("ATIVA");
        assertThat(TenantAcessoService.situacao(tenant(true, LocalDate.now().plusDays(10)))).isEqualTo("ATIVA");
        assertThat(TenantAcessoService.situacao(tenant(true, LocalDate.now().minusDays(1)))).isEqualTo("VENCIDA");
        assertThat(TenantAcessoService.situacao(tenant(false, null))).isEqualTo("SUSPENSA");
        // Suspensa vale mais que vencida
        assertThat(TenantAcessoService.situacao(tenant(false, LocalDate.now().minusDays(5)))).isEqualTo("SUSPENSA");
    }

    @Test
    void motivoDoBloqueioPorSituacao() {
        TenantRepository repo = org.mockito.Mockito.mock(TenantRepository.class);
        TenantAcessoService servico = new TenantAcessoService();
        org.springframework.test.util.ReflectionTestUtils.setField(servico, "tenantRepository", repo);

        Tenant ativa = tenant(true, null);
        Tenant suspensa = tenant(false, null);
        Tenant vencida = tenant(true, LocalDate.now().minusDays(2));
        org.mockito.Mockito.when(repo.findById(1L)).thenReturn(java.util.Optional.of(ativa));
        org.mockito.Mockito.when(repo.findById(2L)).thenReturn(java.util.Optional.of(suspensa));
        org.mockito.Mockito.when(repo.findById(3L)).thenReturn(java.util.Optional.of(vencida));
        org.mockito.Mockito.when(repo.findById(4L)).thenReturn(java.util.Optional.empty());

        // Empresa ativa NÃO pode ser bloqueada (bug real: Optional.map com null)
        assertThat(servico.motivoBloqueio(1L, "ADMIN")).isNull();
        assertThat(servico.motivoBloqueio(2L, "ADMIN")).contains("suspensa");
        assertThat(servico.motivoBloqueio(3L, "USUARIO")).contains("vencida");
        assertThat(servico.motivoBloqueio(4L, "ADMIN")).contains("não encontrada");
        // MASTER nunca é bloqueado, nem em empresa suspensa
        assertThat(servico.motivoBloqueio(2L, "MASTER")).isNull();

        // Depois de invalidar, a nova situação vale na hora
        suspensa.setAtivo(true);
        servico.invalidar(2L);
        assertThat(servico.motivoBloqueio(2L, "ADMIN")).isNull();
    }

    @Test
    void limitadorBloqueiaDepoisDeMuitasFalhas() {
        LoginRateLimiter limitador = new LoginRateLimiter();
        String chave = "e:fulano@x.com";

        for (int i = 0; i < LoginRateLimiter.MAX_POR_EMAIL; i++) {
            limitador.verificar(chave, LoginRateLimiter.MAX_POR_EMAIL); // ainda pode tentar
            limitador.falhou(chave);
        }
        assertThatThrownBy(() -> limitador.verificar(chave, LoginRateLimiter.MAX_POR_EMAIL))
                .isInstanceOf(LimiteExcedidoException.class);

        // Outro e-mail não é afetado, e o sucesso zera o contador
        limitador.verificar("e:outro@x.com", LoginRateLimiter.MAX_POR_EMAIL);
        limitador.sucesso(chave);
        limitador.verificar(chave, LoginRateLimiter.MAX_POR_EMAIL);
    }

    @Test
    void loginInformaSePrecisaTrocarSenha() throws Exception {
        JsonNode n = mapper.readTree(mapper.writeValueAsString(
                new AuthResponse("t", "Ana", "ADMIN", 2L, "Clínica", 5L, true)));
        assertThat(n.get("precisaTrocarSenha").asBoolean()).isTrue();
        assertThat(n.get("usuarioId").asLong()).isEqualTo(5L);
    }

    @Test
    void usuarioNaoExpoeSenhaNemObrigacaoDeTroca() throws Exception {
        Usuario u = new Usuario();
        u.setNome("Ana");
        u.setSenha("$2a$10$hash");
        u.setPrecisaTrocarSenha(true);
        String json = mapper.writeValueAsString(u);
        assertThat(json).doesNotContain("senha").doesNotContain("precisaTrocarSenha").doesNotContain("hash");
    }

    @Test
    void conflitoDeValorUnicoTemMensagemPropria() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var unico = handler.integridade(new DataIntegrityViolationException("x", new SQLException("dup", "23505")));
        assertThat(unico.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(unico.getBody().message()).contains("Já existe");

        var emUso = handler.integridade(new DataIntegrityViolationException("x", new SQLException("fk", "23503")));
        assertThat(emUso.getBody().message()).contains("em uso");
    }
}
