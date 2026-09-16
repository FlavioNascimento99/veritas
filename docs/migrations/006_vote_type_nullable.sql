-- Ausentes se posicionam sem direção (vote_type nulo). Contam para o quórum de
-- "todos se posicionaram", mas não influenciam o resultado (calculateResult ignora away = true).
ALTER TABLE tb_votes ALTER COLUMN vote_type DROP NOT NULL;