package br.com.ruan_dev.saas.repositories;

import br.com.ruan_dev.saas.models.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
}
