package br.com.ruan_dev.saas.repositories;

import br.com.ruan_dev.saas.models.AuditoriaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Long> {

    List<AuditoriaLog> findTop30ByTenantAlvoIdOrderByIdDesc(Long tenantAlvoId);
}
