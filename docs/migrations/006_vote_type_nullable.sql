-- Ausentes se posicionam sem direção (vote_type nulo). Contam para o quórum de
-- "todos se posicionaram", mas não influenciam o resultado (calculateResult ignora away = true).
-- IDEMPOTENT: safe to re-run; drops NOT NULL only if still enforced.
DO $$ BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'tb_votes'
      AND column_name = 'vote_type'
      AND is_nullable = 'NO'
  ) THEN
    ALTER TABLE public.tb_votes ALTER COLUMN vote_type DROP NOT NULL;
  END IF;
END $$;