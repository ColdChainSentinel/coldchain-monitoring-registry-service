-- Idempotent bootstrap of one login role per service, run by Terraform as the
-- server administrator. Passwords come from the environment, never from Git.
-- Each role owns its database and its public schema, so Flyway can migrate
-- it, while PUBLIC loses CONNECT on every service database.

\getenv registry_pw REGISTRY_DB_PASSWORD
\getenv telemetry_pw TELEMETRY_DB_PASSWORD
\getenv alerts_pw ALERTS_DB_PASSWORD

SELECT format('CREATE ROLE %I LOGIN', role_name)
FROM (VALUES ('registry_app'), ('telemetry_app'), ('alerts_app')) AS roles(role_name)
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = role_name)
\gexec

ALTER ROLE registry_app WITH LOGIN PASSWORD :'registry_pw';
ALTER ROLE telemetry_app WITH LOGIN PASSWORD :'telemetry_pw';
ALTER ROLE alerts_app WITH LOGIN PASSWORD :'alerts_pw';

-- The administrator must be able to SET ROLE to transfer database ownership.
GRANT registry_app, telemetry_app, alerts_app TO CURRENT_USER;

ALTER DATABASE coldchain OWNER TO registry_app;
ALTER DATABASE coldchain_telemetry OWNER TO telemetry_app;
ALTER DATABASE coldchain_alerts OWNER TO alerts_app;

REVOKE ALL ON DATABASE coldchain, coldchain_telemetry, coldchain_alerts FROM PUBLIC;

-- Azure creates the public schema of each database owned by azure_pg_admin
-- (not pg_database_owner), so ownership must be handed over explicitly.
\connect coldchain
ALTER SCHEMA public OWNER TO registry_app;
\connect coldchain_telemetry
ALTER SCHEMA public OWNER TO telemetry_app;
\connect coldchain_alerts
ALTER SCHEMA public OWNER TO alerts_app;
