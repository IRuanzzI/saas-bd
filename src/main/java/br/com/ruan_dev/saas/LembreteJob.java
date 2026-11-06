package br.com.ruan_dev.saas;

import br.com.ruan_dev.saas.models.Agendamento;
import br.com.ruan_dev.saas.repositories.AgendamentoRepository;
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

    //@Scheduled(cron = "0 0/30 * * * *")
    @Scheduled(cron = "*/5 * * * * *")
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
                String nomeInstancia = "tenant_" + agendamento.getTenant().getId();

                String mensagem = "Lembrete: Olá " + agendamento.getCliente().getNome() +
                        ", você tem um agendamento (" + dataFormatada + "). " +
                        "Responda 'S' para confirmar ou 'N' para cancelar.";

                whatsAppService.enviarMensagemTexto(
                        nomeInstancia,
                        agendamento.getCliente().getTelefone(),
                        mensagem
                );
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
