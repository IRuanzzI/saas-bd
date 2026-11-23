package br.com.ruan_dev.saas.repositories;

import br.com.ruan_dev.saas.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

   Optional<Usuario> findByEmail(String email);

   // Nativas de propósito: ignoram o filtro de empresa do Hibernate, para
   // checar e-mail repetido em TODAS as empresas e para o MASTER alcançar o
   // admin de qualquer empresa.

   @Query(value = "select count(*) > 0 from usuario where lower(email) = lower(:email)", nativeQuery = true)
   boolean existeEmail(@Param("email") String email);

   @Query(value = "select * from usuario where tenant_id = :tenantId and role = 'ADMIN' order by id limit 1",
           nativeQuery = true)
   Optional<Usuario> findPrimeiroAdmin(@Param("tenantId") Long tenantId);
}
