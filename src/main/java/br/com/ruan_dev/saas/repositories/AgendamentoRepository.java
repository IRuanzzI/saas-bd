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
}
