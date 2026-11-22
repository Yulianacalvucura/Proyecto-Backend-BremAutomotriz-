set -euo pipefail

: "${USUARIOS_DB_PASSWORD:?Falta USUARIOS_DB_PASSWORD en el entorno}"
: "${TALLER_DB_PASSWORD:?Falta TALLER_DB_PASSWORD en el entorno}"

export PGPASSWORD="${POSTGRES_PASSWORD:-}"

psql -v ON_ERROR_STOP=1 \
  --username "${POSTGRES_USER}" \
  --dbname "${POSTGRES_DB}" \
  -v usuarios_pass="${USUARIOS_DB_PASSWORD}" \
  -v taller_pass="${TALLER_DB_PASSWORD}" <<'SQL'
-- Roles (LOGIN para que los servicios puedan conectarse).
CREATE ROLE brem_usuarios_svc LOGIN PASSWORD :'usuarios_pass';
CREATE ROLE brem_taller_svc   LOGIN PASSWORD :'taller_pass';

-- Schema public: se revoca a PUBLIC y se concede USAGE solo a estos roles.
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO brem_usuarios_svc;
GRANT USAGE ON SCHEMA public TO brem_taller_svc;

-- auth-usuarios-service: sin DELETE (los usuarios se desactivan con activo = 0).
GRANT SELECT, INSERT, UPDATE ON TABLE usuarios TO brem_usuarios_svc;

-- taller-service: clientes, vehiculos y ordenes sin DELETE; lineas si;
-- evidencias e historial solo lectura + INSERT; vista solo lectura.
GRANT SELECT, INSERT, UPDATE ON TABLE clientes, vehiculos, ordenes_trabajo TO brem_taller_svc;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE lineas_ot TO brem_taller_svc;
GRANT SELECT, INSERT ON TABLE evidencias_ot, historial_estados TO brem_taller_svc;
GRANT SELECT ON TABLE vista_ordenes TO brem_taller_svc;

-- No hacen falta GRANT USAGE ON SEQUENCE: los IDs son IDENTITY, no serial.
SQL
