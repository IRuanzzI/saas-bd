package br.com.ruan_dev.saas;

import br.com.ruan_dev.saas.models.Agendamento;
import br.com.ruan_dev.saas.repositories.AgendamentoRepository;
import br.com.ruan_dev.saas.services.MensagemWhatsappService;
import br.com.ruan_dev.saas.services.WhatsAppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class LembreteJob {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private WhatsAppService whatsAppService;

    @Autowired
    private MensagemWhatsappService mensagemWhatsappService;

    //@Scheduled(cron = "0 0/30 * * * *")
    @Scheduled(cron = "0 0/5 * * * *")
    public void DispararLembretes() {
       LocalDateTime agora = LocalDateTime.now();
       LocalDateTime daqui24Horas = agora.plusHours(24);

        DateTimeFormatter formatterBanco = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String agoraStr = agora.format(formatterBanco);
        String daquiA24hStr = daqui24Horas.format(formatterBanco);

        List<Agendamento> agendamentos = agendamentoRepository
                .findAgendamentosParaLembrete(agoraStr, daquiA24hStr);

        for (Agendamento agendamento : agendamentos) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String dataFormatada = LocalDateTime.parse(agendamento.getData_hora_inicio()).format(formatter);
                String nomeInstancia = WhatsAppService.nomeInstancia(agendamento.getTenant().getId());

                String mensagem = "Lembrete: Olá " + agendamento.getCliente().getNome() +
                        ", você tem um agendamento (" + dataFormatada + "). " +
                        "Responda 'Sim/S' para confirmar ou 'Não/N' para cancelar.";

                String idMensagem = whatsAppService.enviarMensagemTexto(
                        nomeInstancia,
                        agendamento.getCliente().getTelefone(),
                        mensagem
                );
                // O ID permite reconhecer o cliente se ele responder citando este lembrete.
                mensagemWhatsappService.registrarEnvio(
                        agendamento.getTenant().getId(),
                        idMensagem,
                        agendamento.getId(),
                        agendamento.getCliente().getId(),
                        MensagemWhatsappService.LEMBRETE);
                System.out.println("Lembrete enviado para Agendamento ID: " + agendamento.getId());

            } catch (Exception e) {
                System.err.println("Falha ao enviar lembrete do Agendamento ID " + agendamento.getId() + " (Número inválido?)");
            } finally {
                // Garante que o agendamento saia da fila, mesmo se o número não tiver WhatsApp
                agendamento.setLembreteEnviado(true);
                agendamentoRepository.save(agendamento);
            }
        }
    }



}
