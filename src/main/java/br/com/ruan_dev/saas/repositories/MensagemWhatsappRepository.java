package br.com.ruan_dev.saas.repositories;

import br.com.ruan_dev.saas.models.MensagemWhatsapp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MensagemWhatsappRepository extends JpaRepository<MensagemWhatsapp, Long> {

    Optional<MensagemWhatsapp> findFirstByTenantIdAndMessageId(Long tenantId, String messageId);
}
