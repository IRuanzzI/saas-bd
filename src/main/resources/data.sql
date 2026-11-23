-- Seed de desenvolvimento. Roda a cada inicialização (spring.sql.init.mode=always),
-- por isso cada INSERT só acontece se o registro ainda não existir.

INSERT INTO tenant (nome_fantasia, documento, ativo)
SELECT 'SaaS do Ruan', '000111222000133', true
WHERE NOT EXISTS (SELECT 1 FROM tenant WHERE documento = '000111222000133');

-- Usuário MASTER com a senha '123456' (hash BCrypt), ligado à empresa acima
INSERT INTO usuario (tenant_id, nome, email, senha, role)
SELECT t.id, 'Ruan Pablo', 'admin@saas.com', '$2a$10$RdSBbGyS5zwM0rNbvROHJu5xuReB8.11TOl6ZGHSvc72WbB1XNmee', 'MASTER'
FROM tenant t
WHERE t.documento = '000111222000133'
  AND NOT EXISTS (SELECT 1 FROM usuario WHERE email = 'admin@saas.com');
