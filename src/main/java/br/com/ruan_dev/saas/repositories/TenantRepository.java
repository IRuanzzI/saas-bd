package br.com.ruan_dev.saas.repositories;

import br.com.ruan_dev.saas.models.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    boolean existsByDocumento(String documento);

    // As consultas abaixo são nativas de propósito: o filtro de empresa do
    // Hibernate só vale para HQL, e o MASTER precisa contar dados de qualquer
    // empresa, não só da dele.

    @Query(value = "select count(*) from cliente where tenant_id = :id", nativeQuery = true)
    long contarClientes(@Param("id") Long id);

    @Query(value = "select count(*) from agendamento where tenant_id = :id", nativeQuery = true)
    long contarAgendamentos(@Param("id") Long id);

    @Query(value = "select nome from usuario where tenant_id = :id and role = 'ADMIN' order by id limit 1",
            nativeQuery = true)
    Optional<String> nomeDoAdmin(@Param("id") Long id);

    @Query(value = "select email from usuario where tenant_id = :id and role = 'ADMIN' order by id limit 1",
            nativeQuery = true)
    Optional<String> emailDoAdmin(@Param("id") Long id);
}
