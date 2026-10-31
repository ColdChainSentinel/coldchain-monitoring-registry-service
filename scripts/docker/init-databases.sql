-- ====================================================================
-- ColdChain Sentinel - Inicialización de Bases de Datos Multidominio
-- Ejecutado automáticamente por PostgreSQL al primer arranque
-- (solo si el volumen postgres_data está vacío).
-- La base de datos 'coldchain' se crea automáticamente mediante POSTGRES_DB.
--
-- El script corre con el usuario definido en POSTGRES_USER (${DB_USER}),
-- que es superusuario en la imagen oficial; por eso las bases creadas aquí
-- le pertenecen (OWNER) y no se necesitan GRANT con un nombre de rol fijo.
-- Así el script funciona aunque se cambie DB_USER en el .env.
-- ====================================================================

-- Crear base de datos para el microservicio de Ingesta de Telemetría
CREATE DATABASE coldchain_telemetry;

-- Crear base de datos para el microservicio de Gestión de Alertas
CREATE DATABASE coldchain_alerts;
