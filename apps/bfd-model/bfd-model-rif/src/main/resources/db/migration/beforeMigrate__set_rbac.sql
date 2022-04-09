/*
  This callback runs once, just before flyway executes any pending migrations, and ensures each of our schemas have the
  appropriate roles assigned.
*/

${logic.psql-only} DO $$
${logic.psql-only} BEGIN
${logic.psql-only}   -- add bfd roles to the public schema
${logic.psql-only}   IF EXISTS (SELECT FROM pg_namespace WHERE nspname = 'public') THEN
${logic.psql-only}     PERFORM create_role_if_not_exists('bfd_reader_role');
${logic.psql-only}     PERFORM create_role_if_not_exists('bfd_writer_role');
${logic.psql-only}     PERFORM create_role_if_not_exists('bfd_migrator_role');
${logic.psql-only}     PERFORM add_reader_role_to_schema('bfd_reader_role', 'public');
${logic.psql-only}     PERFORM add_writer_role_to_schema('bfd_writer_role', 'public');
${logic.psql-only}     PERFORM add_migrator_role_to_schema('bfd_migrator_role', 'public');
${logic.psql-only}   END IF;
${logic.psql-only}
${logic.psql-only}   -- add paca roles to the pre_adj schema
${logic.psql-only}   IF EXISTS (SELECT FROM pg_namespace WHERE nspname = 'pre_adj') THEN
${logic.psql-only}     PERFORM create_role_if_not_exists('paca_reader_role');
${logic.psql-only}     PERFORM create_role_if_not_exists('paca_writer_role');
${logic.psql-only}     PERFORM create_role_if_not_exists('paca_migrator_role');
${logic.psql-only}     PERFORM add_reader_role_to_schema('paca_reader_role', 'pre_adj');
${logic.psql-only}     PERFORM add_writer_role_to_schema('paca_writer_role', 'pre_adj');
${logic.psql-only}     PERFORM add_migrator_role_to_schema('paca_migrator_role', 'pre_adj');
${logic.psql-only}   END IF;
${logic.psql-only}
${logic.psql-only}   -- add paca roles to the rda schema
${logic.psql-only}   IF EXISTS (SELECT FROM pg_namespace WHERE nspname = 'rda') THEN
${logic.psql-only}     PERFORM create_role_if_not_exists('paca_reader_role');
${logic.psql-only}     PERFORM create_role_if_not_exists('paca_writer_role');
${logic.psql-only}     PERFORM create_role_if_not_exists('paca_migrator_role');
${logic.psql-only}     PERFORM add_reader_role_to_schema('paca_reader_role', 'rda');
${logic.psql-only}     PERFORM add_writer_role_to_schema('paca_writer_role', 'rda');
${logic.psql-only}     PERFORM add_migrator_role_to_schema('paca_migrator_role', 'rda');
${logic.psql-only}   END IF;
${logic.psql-only} END
${logic.psql-only} $$ LANGUAGE plpgsql;
