
\set ON_ERROR_STOP on

\if :{?usuarios_pass}
\else
  \echo ERROR: falta usuarios_pass (-v usuarios_pass='...')
  \quit
\endif

\if :{?taller_pass}
\else
  \echo ERROR: falta taller_pass (-v taller_pass='...')
  \quit
\endif

CREATE ROLE brem_usuarios_svc LOGIN PASSWORD :'usuarios_pass';
CREATE ROLE brem_taller_svc   LOGIN PASSWORD :'taller_pass';

REVOKE ALL ON SCHEMA public FROM PUBLIC;

GRANT USAGE ON SCHEMA public TO brem_usuarios_svc;
GRANT USAGE ON SCHEMA public TO brem_taller_svc;

GRANT SELECT, INSERT, UPDATE ON TABLE usuarios TO brem_usuarios_svc;

GRANT SELECT, INSERT, UPDATE ON TABLE clientes, vehiculos, ordenes_trabajo TO brem_taller_svc;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE lineas_ot TO brem_taller_svc;
GRANT SELECT, INSERT ON TABLE evidencias_ot, historial_estados TO brem_taller_svc;
GRANT SELECT ON TABLE vista_ordenes TO brem_taller_svc;
