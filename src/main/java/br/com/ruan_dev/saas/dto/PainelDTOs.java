package br.com.ruan_dev.saas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** DTOs do painel do MASTER e da troca de senha. */
public final class PainelDTOs {

    private PainelDTOs() {
    }

    /** Resumo de uma empresa para a lista do painel. */
    public record TenantResumo(
            Long id,
            String nomeFantasia,
            String documento,
            boolean ativo,
            String plano,
            String venceEm,
            /** ATIVA, SUSPENSA ou VENCIDA. */
            String situacao,
            long totalClientes,
            long totalAgendamentos,
            String adminNome,
            String adminEmail) {
    }

    public record TenantAtualizar(
            @NotBlank(message = "O nome da empresa é obrigatório") String nomeFantasia,
            String plano,
            /** yyyy-MM-dd; vazio remove o vencimento. */
            String venceEm) {
    }

    public record Ativo(@NotNull(message = "Informe se a empresa fica ativa") Boolean ativo) {
    }

    public record SenhaTemporaria(String email, String senhaTemporaria) {
    }

    public record AlterarSenha(
            @NotBlank(message = "Informe a senha atual") String senhaAtual,
            @NotBlank(message = "Informe a nova senha")
            @Size(min = 8, message = "A nova senha precisa ter pelo menos 8 caracteres") String novaSenha) {
    }

    public record Auditoria(Long id, String quando, String ator, String acao, String detalhe) {
    }

    public record WhatsAppStatus(String instancia, String estado, boolean conectado) {
    }

    public record WhatsAppQr(String instancia, String estado, boolean conectado,
                             String qrBase64, String codigoPareamento) {
    }
}
