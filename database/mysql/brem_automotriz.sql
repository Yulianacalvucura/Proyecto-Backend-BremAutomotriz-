-- =====================================================================
-- BREM AUTOMOTRIZ - Base de datos MySQL compartida (backend con 3 microservicios)
-- Motor: MySQL 8.0.18 o superior (CHECK y contrasenas aleatorias)
-- Basada en las entidades del proyecto Android (Room): Usuario, Cliente,
-- Vehiculo, OrdenTrabajo, LineaOt y EvidenciaOt.
--
-- Como ejecutarlo (con un usuario administrador, por ejemplo root):
--   mysql -u root -p < brem_automotriz.sql
--
-- Que microservicio usa que tablas:
--   ms-usuarios    -> usuarios
--   ms-clientes    -> clientes, vehiculos
--   ms-ordenes     -> ordenes_trabajo, lineas_ot, evidencias_ot
-- =====================================================================

CREATE DATABASE brem_automotriz
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE brem_automotriz;

-- ---------------------------------------------------------------------
-- 1) TABLAS
-- ---------------------------------------------------------------------

-- MICROSERVICIO USUARIOS
-- Seguridad: NUNCA se guarda la contrasena en texto plano (en la app
-- Android si se hacia). Aqui se guarda solo el HASH (BCrypt, 60 caracteres).
CREATE TABLE usuarios (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(100)    NOT NULL,
    email          VARCHAR(100)    NOT NULL,
    password_hash  VARCHAR(255)    NOT NULL,
    rol            ENUM('ADMIN', 'JEFE') NOT NULL,
    activo         BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_usuarios_email (email),
    CONSTRAINT ck_usuarios_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
    CONSTRAINT ck_usuarios_email  CHECK (email LIKE '%_@_%._%')
) ENGINE = InnoDB;

-- MICROSERVICIO CLIENTES
CREATE TABLE clientes (
    id        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    nombre    VARCHAR(100)    NOT NULL,
    run       VARCHAR(12)     NULL,          -- en la app el RUN es opcional
    telefono  VARCHAR(20)     NULL,
    correo    VARCHAR(100)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_clientes_run (run),        -- varios NULL permitidos
    CONSTRAINT ck_clientes_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) > 0)
) ENGINE = InnoDB;

CREATE TABLE vehiculos (
    id          BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT,
    patente     CHAR(6)           NOT NULL,
    marca       VARCHAR(50)       NOT NULL,
    modelo      VARCHAR(50)       NOT NULL,
    anio        SMALLINT UNSIGNED NOT NULL,
    vin         VARCHAR(17)       NULL,
    tipo        VARCHAR(30)       NULL,      -- Auto, Camioneta, etc.
    cliente_id  BIGINT UNSIGNED   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_vehiculos_patente (patente),
    KEY idx_vehiculos_cliente (cliente_id),
    CONSTRAINT fk_vehiculos_cliente
        FOREIGN KEY (cliente_id) REFERENCES clientes (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_vehiculos_patente CHECK (REGEXP_LIKE(patente, '^[A-Z0-9]{6}$', 'c')),
    CONSTRAINT ck_vehiculos_anio    CHECK (anio BETWEEN 1950 AND 2100)
) ENGINE = InnoDB;

-- MICROSERVICIO ORDENES
-- Cambio respecto a la app: ya NO se repiten patente ni nombre del cliente
-- dentro de la orden (eso duplicaba datos). Se obtienen con JOIN o con la
-- vista vista_ordenes.
CREATE TABLE ordenes_trabajo (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    vehiculo_id          BIGINT UNSIGNED NOT NULL,
    creado_por           BIGINT UNSIGNED NOT NULL,   -- usuario que creo la OT
    fecha_ingreso        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_registro       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    kilometraje          INT UNSIGNED    NOT NULL,
    observacion_ingreso  VARCHAR(500)    NOT NULL,
    observacion_entrega  VARCHAR(500)    NULL,
    estado               ENUM('PENDIENTE', 'EN_PROCESO', 'LISTO', 'ENTREGADO', 'ANULADA')
                                         NOT NULL DEFAULT 'PENDIENTE',
    neto                 INT UNSIGNED    NOT NULL DEFAULT 0,
    iva                  INT UNSIGNED    NOT NULL DEFAULT 0,
    porcentaje_iva       DECIMAL(5,2)    NOT NULL DEFAULT 19.00,
    total                INT UNSIGNED    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_ot_vehiculo (vehiculo_id),
    KEY idx_ot_creado_por (creado_por),
    KEY idx_ot_estado (estado),
    CONSTRAINT fk_ot_vehiculo
        FOREIGN KEY (vehiculo_id) REFERENCES vehiculos (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_ot_usuario
        FOREIGN KEY (creado_por) REFERENCES usuarios (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_ot_kilometraje CHECK (kilometraje <= 1999999),
    CONSTRAINT ck_ot_total       CHECK (total = neto + iva),
    CONSTRAINT ck_ot_porcentaje_iva CHECK (porcentaje_iva BETWEEN 0.00 AND 100.00)
) ENGINE = InnoDB;

-- Historial de cambios de estado de cada orden de trabajo.
CREATE TABLE historial_estados (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ot_id            BIGINT UNSIGNED NOT NULL,
    estado_anterior  ENUM('PENDIENTE', 'EN_PROCESO', 'LISTO', 'ENTREGADO', 'ANULADA') NULL,
    estado_nuevo     ENUM('PENDIENTE', 'EN_PROCESO', 'LISTO', 'ENTREGADO', 'ANULADA') NOT NULL,
    fecha_cambio     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacion      VARCHAR(500) NULL,
    PRIMARY KEY (id),
    KEY idx_historial_ot (ot_id),
    CONSTRAINT fk_historial_ot
        FOREIGN KEY (ot_id) REFERENCES ordenes_trabajo (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE lineas_ot (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ot_id        BIGINT UNSIGNED NOT NULL,
    tipo         ENUM('OM', 'OMEX', 'REPUESTO', 'INSUMO') NOT NULL,
    descripcion  VARCHAR(100)    NOT NULL,
    costo        INT UNSIGNED    NOT NULL,                  -- costo interno
    factor       DECIMAL(3,2)    NOT NULL DEFAULT 1.30,     -- recargo OMEX/REPUESTO
    cobrado      INT UNSIGNED    NOT NULL,                  -- lo que se cobra al cliente
    PRIMARY KEY (id),
    KEY idx_lineas_ot (ot_id),
    CONSTRAINT fk_lineas_ot
        FOREIGN KEY (ot_id) REFERENCES ordenes_trabajo (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT ck_lineas_costo  CHECK (costo > 0),
    CONSTRAINT ck_lineas_factor CHECK (factor >= 1.00),
    CONSTRAINT ck_lineas_desc   CHECK (CHAR_LENGTH(TRIM(descripcion)) > 0)
) ENGINE = InnoDB;

-- Fotos: la base de datos guarda solo la RUTA o URL del archivo,
-- la imagen en si se guarda en disco o en un servicio de archivos.
CREATE TABLE evidencias_ot (
    id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ot_id   BIGINT UNSIGNED NOT NULL,
    tipo    ENUM('VEHICULO', 'NIVEL', 'DOCUMENTO') NOT NULL DEFAULT 'VEHICULO',
    ruta    VARCHAR(255)    NOT NULL,
    fecha   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_evidencias_ot (ot_id),
    CONSTRAINT fk_evidencias_ot
        FOREIGN KEY (ot_id) REFERENCES ordenes_trabajo (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 2) VISTA para la busqueda (patente, cliente o N° de OT)
--    Reemplaza las columnas patente y nombreCliente que estaban repetidas.
-- ---------------------------------------------------------------------
CREATE
    SQL SECURITY DEFINER
    VIEW vista_ordenes AS
SELECT
    o.id            AS ot_id,
    v.patente       AS patente,
    c.nombre        AS nombre_cliente,
    o.estado        AS estado,
    o.fecha_ingreso AS fecha_ingreso,
    o.total         AS total,
    o.vehiculo_id   AS vehiculo_id
FROM ordenes_trabajo o
JOIN vehiculos v ON v.id = o.vehiculo_id
JOIN clientes  c ON c.id = v.cliente_id;

-- ---------------------------------------------------------------------
-- 3) SEGURIDAD: un usuario de MySQL por microservicio (minimo privilegio)
--    Cada servicio solo toca SUS tablas y solo con los permisos que necesita.
--    IMPORTANTE: cambia las contrasenas antes de usar y NO las subas a GitHub
--    (en Spring Boot guardalas en variables de entorno).
-- ---------------------------------------------------------------------
-- MySQL genera contrasenas aleatorias y las muestra al ejecutar cada sentencia.
-- Guardalas en un gestor seguro; no las copies al script ni las subas a GitHub.
-- Si los microservicios estan en otro servidor, reemplaza 'localhost' por un
-- origen restringido y adecuado a tu infraestructura (no uses '%' sin necesidad).
CREATE USER IF NOT EXISTS 'brem_usuarios_svc'@'localhost' IDENTIFIED BY RANDOM PASSWORD;
CREATE USER IF NOT EXISTS 'brem_clientes_svc'@'localhost' IDENTIFIED BY RANDOM PASSWORD;
CREATE USER IF NOT EXISTS 'brem_ordenes_svc'@'localhost' IDENTIFIED BY RANDOM PASSWORD;

-- ms-usuarios: sin DELETE (los usuarios se desactivan con activo = 0)
GRANT SELECT, INSERT, UPDATE ON brem_automotriz.usuarios TO 'brem_usuarios_svc'@'localhost';

-- ms-clientes: sin DELETE
GRANT SELECT, INSERT, UPDATE ON brem_automotriz.clientes  TO 'brem_clientes_svc'@'localhost';
GRANT SELECT, INSERT, UPDATE ON brem_automotriz.vehiculos TO 'brem_clientes_svc'@'localhost';

-- ms-ordenes: las ordenes no se borran (se anulan); solo las lineas se pueden eliminar
GRANT SELECT, INSERT, UPDATE         ON brem_automotriz.ordenes_trabajo TO 'brem_ordenes_svc'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE ON brem_automotriz.lineas_ot       TO 'brem_ordenes_svc'@'localhost';
GRANT SELECT, INSERT                 ON brem_automotriz.evidencias_ot   TO 'brem_ordenes_svc'@'localhost';
GRANT SELECT, INSERT                 ON brem_automotriz.historial_estados TO 'brem_ordenes_svc'@'localhost';
GRANT SELECT                         ON brem_automotriz.vista_ordenes   TO 'brem_ordenes_svc'@'localhost';

FLUSH PRIVILEGES;

-- ---------------------------------------------------------------------
-- 4) DATOS DE PRUEBA (inventados, los mismos del README de la app)
--    Contrasenas originales: admin1/admin2 -> admin123 | jefe -> jefe123
--    Aqui van guardadas como hash BCrypt, no en texto plano.
-- ---------------------------------------------------------------------
INSERT INTO usuarios (nombre, email, password_hash, rol) VALUES
 ('Administrador Uno', 'admin1@brem.cl', '$2b$10$r4///wsszdNx3YcaA1QULOsAuwTLdlOuCWnZoyRbjXmrWmRhiswwO', 'ADMIN'),
 ('Administrador Dos', 'admin2@brem.cl', '$2b$10$r4///wsszdNx3YcaA1QULOsAuwTLdlOuCWnZoyRbjXmrWmRhiswwO', 'ADMIN'),
 ('Jefe de Taller',    'jefe@brem.cl',   '$2b$10$n3rnoBjbdA27gZ81EJp9mOLo4eDT.bCo2KUy6UHmJrlSOSBTVadX.', 'JEFE');

INSERT INTO clientes (nombre, run, telefono, correo) VALUES
 ('Cliente Ejemplo Uno',  '11.111.111-1', '+56 9 1111 1111', 'cliente1@ejemplo.cl'),
 ('Cliente Ejemplo Dos',  '22.222.222-2', '+56 9 2222 2222', 'cliente2@ejemplo.cl');

INSERT INTO vehiculos (patente, marca, modelo, anio, tipo, cliente_id) VALUES
 ('ABCD12', 'Toyota',    'Yaris', 2018, 'Auto',      1),
 ('WXYZ34', 'Chevrolet', 'Sail',  2020, 'Auto',      1),
 ('BCDF56', 'Nissan',    'NP300', 2016, 'Camioneta', 2);

-- Una OT de ejemplo (mismos montos del test CalculadoraMontosTest de la app):
-- neto 94.600 + IVA 17.974 = total 112.574
INSERT INTO ordenes_trabajo
    (vehiculo_id, creado_por, kilometraje, observacion_ingreso, estado, neto, iva, total)
VALUES
    (1, 3, 85000, 'Cambio de aceite y revision de frenos', 'EN_PROCESO', 94600, 17974, 112574);

INSERT INTO historial_estados (ot_id, estado_anterior, estado_nuevo, observacion) VALUES
  (1, 'PENDIENTE', 'EN_PROCESO', 'Inicio de los trabajos de la orden');

INSERT INTO lineas_ot (ot_id, tipo, descripcion, costo, factor, cobrado) VALUES
 (1, 'OM',       'Mano de obra',          30000, 1.00, 30000),
 (1, 'OMEX',     'Rectificado de discos', 14000, 1.30, 18200),
 (1, 'REPUESTO', 'Pastillas de freno',    28000, 1.30, 36400),
 (1, 'INSUMO',   'Insumos de taller',     10000, 1.00, 10000);

INSERT INTO evidencias_ot (ot_id, tipo, ruta) VALUES
 (1, 'VEHICULO', 'evidencias/ot_1_ejemplo.jpg');
