package br.com.ruan_dev.saas.repositories;

import br.com.ruan_dev.saas.models.Agendamento;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Query("""
            select a
            from Agendamento a
            where a.data_hora_inicio like concat(:data, '%')
            order by a.data_hora_inicio
            """)
    List<Agendamento> findAgendamentosHoje(@Param("data") String data);
    // Lembrete 24h antes para quem ainda não respondeu (PENDENTE): o cliente
    // responde S para confirmar ou N para cancelar.
    @Query("SELECT a FROM Agendamento a WHERE a.status = 'PENDENTE' " +
            "AND a.lembreteEnviado = false " +
            "AND a.data_hora_inicio BETWEEN :agora AND :daquiA24Horas")
    List<Agendamento> findAgendamentosParaLembrete(
            @Param("agora") String agora,
            @Param("daquiA24Horas") String daquiA24Horas
    );

    /**
     * Agendamentos de UMA empresa que já receberam lembrete e ainda podem
     * receber resposta (não cancelados/finalizados e que não passaram há muito
     * tempo), do mais próximo para o mais distante.
     *
     * Nativa de propósito: o webhook não tem usuário logado, então o filtro de
     * empresa do Hibernate não vale aqui; a empresa vem do nome da instância.
     */
    @Query(value = "SELECT a.* FROM agendamento a " +
            "WHERE a.tenant_id = :tenantId " +
            "AND a.lembrete_enivado = true " +
            "AND a.status NOT IN ('CANCELADO', 'FINALIZADO') " +
            "AND a.data_hora_inicio >= :desde " +
            "ORDER BY a.data_hora_inicio ASC", nativeQuery = true)
    List<Agendamento> findAguardandoResposta(@Param("tenantId") Long tenantId,
                                             @Param("desde") String desde);

    List<Agendamento> findByClienteId(Long clienteId);
}
