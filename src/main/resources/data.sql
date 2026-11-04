
INSERT INTO tenant (nome_fantasia, documento, ativo)
VALUES ('SaaS do Ruan', '000111222000133', true);

-- Insere o Usuário com a senha '123456' previamente gerada em hash BCrypt
INSERT INTO usuario (tenant_id, nome, email, senha, role)
VALUES (1, 'Ruan Pablo', 'admin@saas.com', '$2a$10$PrQmu9gJoZz3bUy58waDquhbxbK2x1wdtuH5OdGUoGB30a8u8GMcG', 'MASTER');