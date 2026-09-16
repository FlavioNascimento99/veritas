-- fix/unique-identifiers: unique constraints for generated identifiers.
-- Run on Supabase after branch 5 is merged (prod uses ddl-auto: validate).

ALTER TABLE tb_students
    ADD CONSTRAINT uq_students_register UNIQUE (register);

ALTER TABLE tb_professors
    ADD CONSTRAINT uq_professors_register UNIQUE (register);

ALTER TABLE tb_administrator
    ADD CONSTRAINT uq_administrator_register UNIQUE (tb_admin_register);

ALTER TABLE tb_processes
    ADD CONSTRAINT uq_processes_number UNIQUE (number);