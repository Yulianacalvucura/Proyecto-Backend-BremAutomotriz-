-- =====================================================================
-- BREM AUTOMOTRIZ - usuario de MySQL para taller-service
-- Ejecutarlo DESPUÉS de database/mysql/brem_automotriz.sql.
-- MySQL genera la contraseña y la muestra por pantalla: guardarla en el
-- gestor seguro y pasarla como DB_PASSWORD (nunca versionarla).
-- =====================================================================

CREATE USER IF NOT EXISTS 'brem_taller_svc'@'localhost' IDENTIFIED BY RANDOM PASSWORD;

-- Unión de los permisos de brem_clientes_svc y brem_ordenes_svc del script original.
GRANT SELECT, INSERT, UPDATE         ON brem_automotriz.clientes           TO 'brem_taller_svc'@'localhost';
GRANT SELECT, INSERT, UPDATE         ON brem_automotriz.vehiculos          TO 'brem_taller_svc'@'localhost';
GRANT SELECT, INSERT, UPDATE         ON brem_automotriz.ordenes_trabajo    TO 'brem_taller_svc'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON brem_automotriz.lineas_ot          TO 'brem_taller_svc'@'localhost';
GRANT SELECT, INSERT                 ON brem_automotriz.evidencias_ot      TO 'brem_taller_svc'@'localhost';
GRANT SELECT, INSERT                 ON brem_automotriz.historial_estados  TO 'brem_taller_svc'@'localhost';
GRANT SELECT                         ON brem_automotriz.vista_ordenes      TO 'brem_taller_svc'@'localhost';

FLUSH PRIVILEGES;
