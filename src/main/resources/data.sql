-- Insere o Tenant base (A chave primária será gerada automaticamente, assumimos 1 em um banco zerado)
INSERT INTO tenant (nome_fantasia, documento, ativo)
VALUES ('SaaS do Ruan', '000111222000133', true);

-- Insere o Usuário com a senha '123456' previamente gerada em hash BCrypt
INSERT INTO usuario (tenant_id, nome, email, senha, role)
VALUES (1, 'Ruan Pablo', 'admin@saas.com', '$2a$12$wS3.F3.Wp34pY.V8J9h4..Q4Lw2QO5i4O8kZ2O2mYx.9vO1B/7j6m', 'ADMIN');