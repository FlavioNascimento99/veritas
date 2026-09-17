-- 007_rls_hardening.sql
-- Endurece o schema public do Supabase para as advertências do Dashboard:
--   * "RLS Disabled"                 -> ENABLE ROW LEVEL SECURITY em todas as tabelas
--   * "GraphQL / Sensitive Columns"  -> revoga privilégios de anon/authenticated,
--                                        removendo as tabelas e colunas do PostgREST/GraphQL
--
-- O app Spring Boot conecta como "postgres" (dono das tabelas -> BYPASSRLS);
-- habilitar RLS não afeta a aplicação. O service_role (CLI/backend) mantém acesso.
-- Apenas o acesso público via chave anon/authenticated é cortado.

BEGIN;

-- 1) Habilita RLS em todas as tabelas de negócio
ALTER TABLE public.tb_administrator     ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.meeting_participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.members              ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_collegiate        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_courses           ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_subjects          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_students          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_meetings          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_processes         ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_professors        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tb_votes             ENABLE ROW LEVEL SECURITY;

-- 2) anon/authenticated perdem acesso ao schema public (some do GraphQL/PostgREST)
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
    REVOKE ALL ON ALL TABLES    IN SCHEMA public FROM anon;
    REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM anon;
    REVOKE ALL ON ALL FUNCTIONS IN SCHEMA public FROM anon;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
    REVOKE ALL ON ALL TABLES    IN SCHEMA public FROM authenticated;
    REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM authenticated;
    REVOKE ALL ON ALL FUNCTIONS IN SCHEMA public FROM authenticated;
  END IF;
END $$;

-- 3) Objetos criados no futuro também não herdam exposição pública
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'anon') THEN
    ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES    FROM anon;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM anon;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON FUNCTIONS FROM anon;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
    ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON TABLES    FROM authenticated;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON SEQUENCES FROM authenticated;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public REVOKE ALL ON FUNCTIONS FROM authenticated;
  END IF;
END $$;

-- 4) service_role (CLI/Supabase backend) mantém acesso total
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'service_role') THEN
    GRANT ALL ON ALL TABLES    IN SCHEMA public TO service_role;
    GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO service_role;
    GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO service_role;
  END IF;
END $$;

COMMIT;