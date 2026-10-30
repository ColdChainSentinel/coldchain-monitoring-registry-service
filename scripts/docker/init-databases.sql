-- ====================================================================
-- ColdChain Sentinel - Inicialización de Bases de Datos Multidominio
-- Ejecutado automáticamente por PostgreSQL al primer arranque.
-- La base de datos 'coldchain' se crea automáticamente mediante POSTGRES_DB.
-- ====================================================================

-- Crear base de datos para el microservicio de Ingesta de Telemetría
CREATE DATABASE coldchain_telemetry;

-- Crear base de datos para el microservicio de Gestión de Alertas
CREATE DATABASE coldchain_alerts;

-- Garantizar permisos al usuario de la aplicación
GRANT ALL PRIVILEGES ON DATABASE coldchain_telemetry TO coldchain;
GRANT ALL PRIVILEGES ON DATABASE coldchain_alerts TO coldchain;

