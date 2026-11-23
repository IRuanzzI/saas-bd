package br.com.ruan_dev.saas;

import br.com.ruan_dev.saas.models.Agendamento;
import br.com.ruan_dev.saas.models.Cliente;
import br.com.ruan_dev.saas.models.MensagemWhatsapp;
import br.com.ruan_dev.saas.repositories.AgendamentoRepository;
import br.com.ruan_dev.saas.repositories.MensagemWhatsappRepository;
import br.com.ruan_dev.saas.services.AgendamentoService;
import br.com.ruan_dev.saas.services.RespostaWhatsAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RespostaWhatsAppServiceTest {

    private static final String LID = "137207025242123@lid";

    private AgendamentoRepository agendamentos;
    private MensagemWhatsappRepository mensagens;
    private AgendamentoService agendamentoService;
    private RespostaWhatsAppService servico;

    @BeforeEach
    void preparar() {
        agendamentos = mock(AgendamentoRepository.class);
        mensagens = mock(MensagemWhatsappRepository.class);
        agendamentoService = mock(AgendamentoService.class);
        servico = new RespostaWhatsAppService();
        ReflectionTestUtils.setField(servico, "agendamentoRepository", agendamentos);
        ReflectionTestUtils.setField(servico, "mensagemRepository", mensagens);
        ReflectionTestUtils.setField(servico, "agendamentoService", agendamentoService);
    }

    private static Cliente cliente(long id, String nome, String telefone, String lid) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNome(nome);
        c.setTelefone(telefone);
        c.setWhatsappLid(lid);
        return c;
    }

    private static Agendamento agendamento(long id, Cliente c, String status) {
        Agendamento a = new Agendamento();
        a.setId(id);
        a.setCliente(c);
        a.setStatus(status);
        a.setLembreteEnviado(true);
        a.setData_hora_inicio("2026-11-23T10:00:00");
        return a;
    }

    private void aguardando(Agendamento... lista) {
        when(agendamentos.findAguardandoResposta(eq(6L), anyString())).thenReturn(List.of(lista));
    }

    // ------------------------------------------------------------ regras puras

    @Test
    void interpretaRespostasDoCliente() {
        for (String s : List.of("S", "s", "Sim", "SIM!", " sim ", "Sim, confirmo", "confirmo", "Confirmar")) {
            assertThat(RespostaWhatsAppService.interpretar(s)).as(s).isEqualTo("CONFIRMADO");
        }
        for (String s : List.of("N", "n", "Não", "NAO", "nao posso", "Cancelar", "cancelo")) {
            assertThat(RespostaWhatsAppService.interpretar(s)).as(s).isEqualTo("CANCELADO");
        }
        for (String s : List.of("", "   ", "oi", "bom dia", "Sábado serve?", "talvez", "sim é que eu queria saber outra coisa")) {
            assertThat(RespostaWhatsAppService.interpretar(s)).as(s).isNull();
        }
        assertThat(RespostaWhatsAppService.interpretar(null)).isNull();
    }

    @Test
    void telefoneComESemNonoDigitoEhOMesmoNumero() {
        // 12 dígitos (como o WhatsApp manda) x 13 dígitos (com o 9 extra) x com máscara
        assertThat(RespostaWhatsAppService.chaveTelefone("553899884360"))
                .isEqualTo(RespostaWhatsAppService.chaveTelefone("5538999884360"))
                .isEqualTo(RespostaWhatsAppService.chaveTelefone("(38) 9988-4360"));
        assertThat(RespostaWhatsAppService.chaveTelefone("5511912345678"))
                .isNotEqualTo(RespostaWhatsAppService.chaveTelefone("5511912345679"));
    }

    @Test
    void nomeDoPerfilCombinaComONomeDoCliente() {
        assertThat(RespostaWhatsAppService.nomeCombina("Maria", "Maria Diniz💕")).isTrue();
        assertThat(RespostaWhatsAppService.nomeCombina("Ana Beatriz Souza", "ana b.")).isTrue();
        assertThat(RespostaWhatsAppService.nomeCombina("José", "Jose Almeida")).isTrue();
        assertThat(RespostaWhatsAppService.nomeCombina("Maria", "Mariana Lima")).isFalse();
        assertThat(RespostaWhatsAppService.nomeCombina("Maria", null)).isFalse();
    }

    @Test
    void empresaVemDoNomeDaInstancia() {
        assertThat(RespostaWhatsAppService.tenantDaInstancia("tenant_6")).isEqualTo(6L);
        assertThat(RespostaWhatsAppService.tenantDaInstancia("tenant_x")).isNull();
        assertThat(RespostaWhatsAppService.tenantDaInstancia("outra")).isNull();
        assertThat(RespostaWhatsAppService.tenantDaInstancia(null)).isNull();
    }

    // ------------------------------------------------------------ o caso real: remetente é um LID

    @Test
    void lidComUmUnicoClienteQueCombinaComONomeConfirmaEAprendeOLid() {
        Cliente maria = cliente(2, "Maria", "553899884360", null);
        aguardando(agendamento(3, maria, "PENDENTE"));

        String r = servico.processar("tenant_6", LID, "Maria Diniz💕", "S", null);

        assertThat(r).startsWith("ok");
        verify(agendamentoService).responderConfirmacao(3L, "CONFIRMADO");
        assertThat(maria.getWhatsappLid()).isEqualTo(LID);
    }

    @Test
    void lidJaAprendidoIdentificaOClienteMesmoComOutroNomeNoPerfil() {
        Cliente maria = cliente(2, "Maria", "553899884360", LID);
        Cliente joao = cliente(3, "João", "5511999990000", null);
        aguardando(agendamento(30, joao, "PENDENTE"), agendamento(31, maria, "PENDENTE"));

        servico.processar("tenant_6", LID, "💕 M 💕", "Sim", null);

        verify(agendamentoService).responderConfirmacao(31L, "CONFIRMADO");
    }

    @Test
    void respostaCitandoOLembreteIdentificaOCliente() {
        Cliente maria = cliente(2, "Maria", "553899884360", null);
        Cliente outraMaria = cliente(3, "Maria", "5511999990000", null);
        aguardando(agendamento(30, outraMaria, "PENDENTE"), agendamento(31, maria, "PENDENTE"));

        MensagemWhatsapp enviada = new MensagemWhatsapp();
        enviada.setClienteId(2L);
        when(mensagens.findFirstByTenantIdAndMessageId(6L, "3EB0ABC")).thenReturn(Optional.of(enviada));

        servico.processar("tenant_6", LID, "Maria", "S", "3EB0ABC");

        verify(agendamentoService).responderConfirmacao(31L, "CONFIRMADO");
        assertThat(maria.getWhatsappLid()).isEqualTo(LID);
        assertThat(outraMaria.getWhatsappLid()).isNull();
    }

    @Test
    void doisClientesComOMesmoNomeNaoSaoConfundidos() {
        aguardando(
                agendamento(30, cliente(2, "Maria Souza", "553899884360", null), "PENDENTE"),
                agendamento(31, cliente(3, "Maria Lima", "5511999990000", null), "PENDENTE"));

        String r = servico.processar("tenant_6", LID, "Maria", "S", null);

        assertThat(r).startsWith("ignorado").contains("mais de um cliente");
        verify(agendamentoService, never()).responderConfirmacao(anyLong(), anyString());
    }

    @Test
    void remetenteDesconhecidoNaoConfirmaAgendamentoDeOutraPessoa() {
        aguardando(agendamento(30, cliente(2, "Carlos", "553899884360", null), "PENDENTE"));

        String r = servico.processar("tenant_6", LID, "Fulana Qualquer", "S", null);

        assertThat(r).startsWith("ignorado");
        verify(agendamentoService, never()).responderConfirmacao(anyLong(), anyString());
    }

    // ------------------------------------------------------------ remetente com telefone

    @Test
    void telefoneNoJidIdentificaOClienteMesmoSemONonoDigito() {
        Cliente maria = cliente(2, "Maria", "5538999884360", null); // cadastro com o 9
        aguardando(agendamento(3, maria, "PENDENTE"));

        servico.processar("tenant_6", "553899884360@s.whatsapp.net", "qualquer", "S", null);

        verify(agendamentoService).responderConfirmacao(3L, "CONFIRMADO");
    }

    @Test
    void usaOAgendamentoMaisProximoDoCliente() {
        Cliente maria = cliente(2, "Maria", "553899884360", null);
        aguardando(agendamento(5, maria, "PENDENTE"), agendamento(9, maria, "PENDENTE"));

        servico.processar("tenant_6", "553899884360@s.whatsapp.net", null, "S", null);

        verify(agendamentoService).responderConfirmacao(5L, "CONFIRMADO");
    }

    // ------------------------------------------------------------ status

    @Test
    void simNaoReconfirmaAgendamentoJaConfirmado() {
        Cliente maria = cliente(2, "Maria", "553899884360", null);
        aguardando(agendamento(3, maria, "CONFIRMADO"));

        String r = servico.processar("tenant_6", "553899884360@s.whatsapp.net", null, "S", null);

        assertThat(r).startsWith("ignorado");
        verify(agendamentoService, never()).responderConfirmacao(anyLong(), anyString());
    }

    @Test
    void clienteQueJaConfirmouPodeCancelar() {
        Cliente maria = cliente(2, "Maria", "553899884360", null);
        aguardando(agendamento(3, maria, "CONFIRMADO"));

        servico.processar("tenant_6", "553899884360@s.whatsapp.net", null, "N", null);

        verify(agendamentoService).responderConfirmacao(3L, "CANCELADO");
    }

    @Test
    void ignoraInstanciaDesconhecidaETextoQueNaoEResposta() {
        assertThat(servico.processar("outra", LID, "Maria", "S", null)).startsWith("ignorado");
        assertThat(servico.processar("tenant_6", LID, "Maria", "oi tudo bem?", null)).startsWith("ignorado");
        verify(agendamentoService, never()).responderConfirmacao(anyLong(), anyString());
        Mockito.verifyNoInteractions(agendamentos);
    }
}
