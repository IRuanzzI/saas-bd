package br.com.ruan_dev.saas.repositories;

import br.com.ruan_dev.saas.models.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {


    List<Agendamento> findAgendamentosHoje();
}
