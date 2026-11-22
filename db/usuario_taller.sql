USE brem_automotriz;

CREATE USER IF NOT EXISTS 'brem_taller_svc'@'localhost' IDENTIFIED BY RANDOM PASSWORD;

GRANT SELECT, INSERT, UPDATE ON brem_automotriz.clientes           TO 'brem_taller_svc'@'localhost';
GRANT SELECT, INSERT, UPDATE ON brem_automotriz.vehiculos          TO 'brem_taller_svc'@'localhost';
GRANT SELECT, INSERT, UPDATE ON brem_automotriz.ordenes_trabajo    TO 'brem_taller_svc'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON brem_automotriz.lineas_ot  TO 'brem_taller_svc'@'localhost';
GRANT SELECT, INSERT              ON brem_automotriz.evidencias_ot   TO 'brem_taller_svc'@'localhost';
GRANT SELECT, INSERT              ON brem_automotriz.historial_estados TO 'brem_taller_svc'@'localhost';
GRANT SELECT                      ON brem_automotriz.vista_ordenes    TO 'brem_taller_svc'@'localhost';

FLUSH PRIVILEGES;
