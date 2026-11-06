package br.com.ruan_dev.saas.repositories;

import br.com.ruan_dev.saas.models.Agendamento;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    @Query("""
            select a
            from Agendamento a
            where a.data_hora_inicio like concat(:data, '%')
            order by a.data_hora_inicio
            """)
    List<Agendamento> findAgendamentosHoje(@Param("data") String data);
    @Query("SELECT a FROM Agendamento a WHERE a.status = 'CONFIRMADO' " +
            "AND a.lembreteEnviado = false " +
            "AND a.data_hora_inicio BETWEEN :agora AND :daquiA24Horas")
    List<Agendamento> findAgendamentosParaLembrete(
            @Param("agora") String agora,
            @Param("daquiA24Horas") String daquiA24Horas
    );


    @Query(value = "SELECT a.* FROM agendamento a INNER JOIN cliente c ON a.cliente_id = c.id " +
            "WHERE c.telefone = :telefone " +
            "AND a.status NOT IN ('CANCELADO', 'FINALIZADO') " +
            "AND a.lembrete_enivado = true " + // <-- Corrigido para bater com o banco
            "ORDER BY a.data_hora_inicio ASC LIMIT 1", nativeQuery = true)
    Optional<Agendamento> findProximoAgendamentoPorTelefone(@Param("telefone") String telefone);
}
