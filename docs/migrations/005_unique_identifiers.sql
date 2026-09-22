-- fix/unique-identifiers: unique constraints for generated identifiers.
-- Run on Supabase after branch 5 is merged (prod uses ddl-auto: validate).
-- IDEMPOTENT: safe to re-run; adds each constraint only if missing.

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_students_register') THEN
    ALTER TABLE public.tb_students
      ADD CONSTRAINT uq_students_register UNIQUE (register);
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_professors_register') THEN
    ALTER TABLE public.tb_professors
      ADD CONSTRAINT uq_professors_register UNIQUE (register);
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_administrator_register') THEN
    ALTER TABLE public.tb_administrator
      ADD CONSTRAINT uq_administrator_register UNIQUE (tb_admin_register);
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_processes_number') THEN
    ALTER TABLE public.tb_processes
      ADD CONSTRAINT uq_processes_number UNIQUE (number);
  END IF;
END $$;