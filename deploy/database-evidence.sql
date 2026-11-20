-- Run with psql -X -v ON_ERROR_STOP=1 -t -A.
-- A deterministic count AND hash for every application table detects changed rows as well as missing data.
-- No names, password hashes, bearer tokens or other row contents are emitted.
SELECT 'postgres_version=' || current_setting('server_version');
SELECT 'application_tables=' || count(*) FROM pg_tables WHERE schemaname = 'public';
SELECT 'application_role_is_not_superuser=' || (NOT rolsuper) FROM pg_roles WHERE rolname = current_user;
SELECT 'stored_passwords_are_bcrypt=' || (count(*) > 0 AND bool_and(password_hash LIKE '$2%')) FROM users;
SELECT format(
  'SELECT %L || '' rows='' || count(*) || '' md5='' || md5(coalesce(string_agg(row_to_json(t)::text, E''\n'' ORDER BY row_to_json(t)::text), '''')) FROM public.%I t;',
  tablename, tablename)
FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename
\gexec
