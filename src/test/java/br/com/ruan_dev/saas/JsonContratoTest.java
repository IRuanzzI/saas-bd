package br.com.ruan_dev.saas;

import br.com.ruan_dev.saas.dto.AuthResponse;
import br.com.ruan_dev.saas.models.Agendamento;
import br.com.ruan_dev.saas.models.Cliente;
import br.com.ruan_dev.saas.models.Tenant;
import br.com.ruan_dev.saas.models.Usuario;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garante o contrato JSON que o app Flutter consome (sem subir Spring nem banco).
 */
class JsonContratoTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void loginDevolveTokenEDadosDoUsuario() throws Exception {
        String json = mapper.writeValueAsString(
                new AuthResponse("jwt", "Ruan", "ADMIN", 4L, "Clínica Vida", 9L, false));
        JsonNode n = mapper.readTree(json);

        assertThat(n.get("token").asString()).isEqualTo("jwt");
        assertThat(n.get("nome").asString()).isEqualTo("Ruan");
        assertThat(n.get("role").asString()).isEqualTo("ADMIN");
        assertThat(n.get("tenantId").asLong()).isEqualTo(4L);
        assertThat(n.get("tenant").asString()).isEqualTo("Clínica Vida");
        assertThat(n.get("usuarioId").asLong()).isEqualTo(9L);
    }

    @Test
    void agendamentoNaoExpoeEmpresaNemHashDeSenha() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setNome_fantasia("Clínica Vida");
        tenant.setDocumento("12345678000199");

        Cliente cliente = new Cliente();
        cliente.setId(3L);
        cliente.setTenant(tenant);
        cliente.setNome("Maria");
        cliente.setTelefone("5511912345678");
        cliente.setEmail("maria@x.com");

        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setTenant(tenant);
        usuario.setNome("Dra. Ana");
        usuario.setEmail("ana@x.com");
        usuario.setSenha("$2a$10$hashqualquer");
        usuario.setRole("ADMIN");

        Agendamento a = new Agendamento();
        a.setId(7L);
        a.setTenant(tenant);
        a.setCliente(cliente);
        a.setUsuario(usuario);
        a.setData_hora_inicio("2026-11-24T10:00:00");
        a.setData_hora_final("2026-11-24T10:45:00");
        a.setStatus("PENDENTE");

        String json = mapper.writeValueAsString(a);

        assertThat(json).doesNotContain("senha").doesNotContain("hashqualquer");
        assertThat(json).doesNotContain("tenant").doesNotContain("12345678000199");

        JsonNode n = mapper.readTree(json);
        assertThat(n.get("data_hora_inicio").asString()).isEqualTo("2026-11-24T10:00:00");
        assertThat(n.get("status").asString()).isEqualTo("PENDENTE");
        assertThat(n.get("lembreteEnviado").asBoolean()).isFalse();
        assertThat(n.get("cliente").get("nome").asString()).isEqualTo("Maria");
        assertThat(n.get("usuario").get("nome").asString()).isEqualTo("Dra. Ana");
    }

    @Test
    void empresaVindaNoCorpoDaRequisicaoEIgnorada() throws Exception {
        Cliente c = mapper.readValue(
                "{\"nome\":\"Bia\",\"telefone\":\"5511\",\"tenant\":{\"id\":99}}", Cliente.class);

        assertThat(c.getNome()).isEqualTo("Bia");
        assertThat(c.getTenant()).isNull();
    }

    @Test
    void agendamentoRecebeClienteSoComId() throws Exception {
        Agendamento a = mapper.readValue(
                "{\"cliente\":{\"id\":3},\"data_hora_inicio\":\"2026-11-24T10:00:00\","
                        + "\"data_hora_final\":\"2026-11-24T10:45:00\",\"tenant\":{\"id\":99}}",
                Agendamento.class);

        assertThat(a.getCliente().getId()).isEqualTo(3L);
        assertThat(a.getData_hora_inicio()).isEqualTo("2026-11-24T10:00:00");
        assertThat(a.getTenant()).isNull();
    }
}
