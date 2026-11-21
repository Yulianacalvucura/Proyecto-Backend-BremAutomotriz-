# Brem Automotriz - Backend

Backend de Brem Automotriz con **Spring Boot 4.2.0 / Java 21** y una única base de datos **MySQL 8 compartida** (`brem_automotriz`).

El esquema lo crea exclusivamente el script SQL ejecutado a mano: no hay Flyway, Liquibase ni DDL de Hibernate (`spring.jpa.hibernate.ddl-auto=none`).

## Servicios

| Servicio | Puerto | Base de datos / responsabilidad |
| --- | --- | --- |
| `Backend/auth-usuarios-service` | **8091** | Solo la tabla `usuarios`. Genera el JWT. |
| `Backend/taller-service` | **8081** | `clientes`, `vehiculos`, `ordenes_trabajo`, `lineas_ot`, `evidencias_ot`, `historial_estados` y la vista `vista_ordenes`. |
| `Backend/api-gateway` | **9090** | Gateway MVC (servlet, no WebFlux). Valida el JWT e inyecta los headers `X-User-*`. No usa base de datos. |

Rutas del gateway (`spring.cloud.gateway.server.webmvc.*`):

- `/auth/**` → `http://localhost:8091`
- `/clientes/**`, `/vehiculos/**`, `/ordenes/**` → `http://localhost:8081`
- Documentación: ver la sección **Documentación (Swagger)** más abajo.

## Variables de entorno

Ninguna contraseña ni secreto real se guarda en el repositorio: todo se lee de variables de entorno.

| Variable | Uso |
| --- | --- |
| `DB_USER` | Usuario de MySQL. Distinto por servicio: `brem_usuarios_svc` en auth, `brem_taller_svc` en taller. |
| `DB_PASSWORD` | Contraseña de ese usuario. Distinta por servicio: la que imprimió MySQL para cada cuenta. No aplica al gateway (no usa BD). |
| `JWT_SECRET` | Secreto para firmar/validar JWT. **El mismo valor en api-gateway y auth-usuarios-service**, con **mínimo 32 caracteres**. No se usa en taller-service. |
| `AUTH_SERVICE_URL` | Opcional. URL base de auth-usuarios-service usada por el gateway. Default: `http://localhost:8091`. |
| `TALLER_SERVICE_URL` | Opcional. URL base de taller-service usada por el gateway. Default: `http://localhost:8081`. |

Ejemplo (PowerShell, sesión de desarrollo):

```powershell
# auth-usuarios-service
$env:DB_USER = "brem_usuarios_svc"
$env:DB_PASSWORD = "<contraseña entregada por MySQL para brem_usuarios_svc>"
$env:JWT_SECRET = "<secreto de al menos 32 caracteres>"

# taller-service (en otra terminal, con la contraseña de su propia cuenta)
$env:DB_USER = "brem_taller_svc"
$env:DB_PASSWORD = "<contraseña entregada por db/usuario_taller.sql>"

# api-gateway (sin BD; el MISMO JWT_SECRET que auth)
$env:JWT_SECRET = "<secreto de al menos 32 caracteres>"
```

## Orden para levantar el proyecto

1. Ejecutar el script del esquema con un usuario administrador (root):

   ```powershell
   mysql -u root -p < database\mysql\brem_automotriz.sql
   ```

   Ese script crea la base, las tablas, la vista `vista_ordenes` y la cuenta `brem_usuarios_svc` (SELECT/INSERT/UPDATE sobre `usuarios`). MySQL imprime su contraseña aleatoria una sola vez: guárdala.

2. Ejecutar el script de la cuenta de taller:

   ```powershell
   mysql -u root -p < db\usuario_taller.sql
   ```

   Crea `brem_taller_svc` con `IDENTIFIED BY RANDOM PASSWORD` y permisos mínimos. Guarda de inmediato la contraseña que imprima MySQL en `DB_PASSWORD` de taller-service.

3. Levantar los servicios en este orden: **auth-usuarios-service (8091)** → **taller-service (8081)** → **api-gateway (9090)**.

   ```powershell
   # ejecutado dentro de cada carpeta Backend/<módulo>
   .\mvnw.cmd spring-boot:run
   ```

## Endpoints

Todas las rutas están expuestas por el gateway en `http://localhost:9090`. `POST /auth/login` y la documentación Swagger son públicos; el resto exige `Authorization: Bearer {{token}}`. Los permisos por verbo usan los roles `ADMIN` y `JEFE`. Los errores de `auth-usuarios-service` y `taller-service` responden `{status, error, mensaje, timestamp}` (401/403/404/400/500); el 401 del gateway responde `{status, error, mensaje}`, sin `timestamp`.

### Autenticación y usuarios

| Método | Ruta | Descripción | Acceso |
| --- | --- | --- | --- |
| `POST` | `/auth/login` | Login; devuelve el JWT. | Público |
| `POST` | `/auth/registrar` | Alta de usuario. | Solo `ADMIN` |
| `GET` | `/auth/usuarios` | Listado de usuarios. | Solo `ADMIN` |
| `PATCH` | `/auth/usuarios/{id}/desactivar` | Desactiva un usuario. | Solo `ADMIN` |
| `PATCH` | `/auth/usuarios/{id}/activar` | Reactiva un usuario. | Solo `ADMIN` |

### Clientes

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/clientes` | Listado. |
| `GET` | `/clientes/{id}` | Detalle. |
| `POST` | `/clientes` | Alta. |
| `PUT` | `/clientes/{id}` | Edición. |

### Vehículos

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/vehiculos` | Listado. |
| `GET` | `/vehiculos/{id}` | Detalle. |
| `GET` | `/vehiculos/cliente/{clienteId}` | Vehículos de un cliente. |
| `GET` | `/vehiculos/patente/{patente}` | Búsqueda por patente. |
| `POST` | `/vehiculos` | Alta. |
| `PUT` | `/vehiculos/{id}` | Edición. |

### Órdenes de trabajo

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/ordenes` | Listado (tabla `ordenes_trabajo`). |
| `GET` | `/ordenes/{id}` | Detalle. |
| `GET` | `/ordenes/buscar?q=` | Búsqueda (vista `vista_ordenes`). |
| `POST` | `/ordenes` | Alta; requiere `X-User-Id`. |
| `PUT` | `/ordenes/{id}` | Edición. |
| `PATCH` | `/ordenes/{id}/estado` | Cambio de estado. |

### Líneas, evidencias e historial

| Método | Ruta | Descripción |
| --- | --- | --- |
| `GET` | `/ordenes/{ordenId}/lineas` | Listado de líneas. |
| `POST` | `/ordenes/{ordenId}/lineas` | Alta de línea. |
| `PUT` | `/ordenes/{ordenId}/lineas/{lineaId}` | Edición de línea. |
| `DELETE` | `/ordenes/{ordenId}/lineas/{lineaId}` | Borrado de línea (único `DELETE`). |
| `GET` | `/ordenes/{ordenId}/evidencias` | Listado de evidencias. |
| `POST` | `/ordenes/{ordenId}/evidencias` | Alta de evidencia. |
| `GET` | `/ordenes/{ordenId}/historial` | Historial de cambios de estado. |

Notas de uso:

- `POST /ordenes` necesita `X-User-Id` (alimenta `creado_por`): el gateway lo inyecta desde el JWT junto con `X-User-Username` y `X-User-Roles`, **no se envían manualmente** por el puerto 9090. Accediendo directo a 8081 sí hay que enviarlos. Sin `X-User-Id` → 401.
- `PATCH /ordenes/{id}/estado` recibe `{"estado":"...","observacionEntrega":"..."}`. `observacionEntrega` es obligatoria y no puede quedar en blanco al pasar a `ENTREGADO` → 400. Estados: `PENDIENTE → EN_PROCESO → LISTO → ENTREGADO`; `ANULADA` desde `PENDIENTE`, `EN_PROCESO` o `LISTO`; `ENTREGADO` y `ANULADA` son finales.
- `total = neto + iva` con redondeo `HALF_UP`; `cobrado` y los montos los calcula el servidor, nunca se confía en lo que envíe el cliente. Factores: `OM`/`INSUMO` 1.00, `OMEX`/`REPUESTO` 1.30. `porcentajeIva` es opcional (default `19.00`, rango `0..100`).
- Las líneas solo se editan si la orden no está `ENTREGADO`/`ANULADA`; las evidencias no tienen ese bloqueo.

## Documentación (Swagger)

| Recurso | URL |
| --- | --- |
| Swagger agrupado del gateway (selector **Auth** / **Taller**) | `http://localhost:9090/swagger-ui.html` |
| Swagger de auth | `http://localhost:9090/auth/swagger-ui.html` |
| OpenAPI de auth | `http://localhost:9090/auth/v3/api-docs` |
| Swagger de taller | `http://localhost:9090/taller/swagger-ui.html` |
| OpenAPI de taller | `http://localhost:9090/taller/v3/api-docs` |

También disponibles en los puertos directos: `http://localhost:8091/auth/swagger-ui.html` y `http://localhost:8081/taller/swagger-ui.html`.

## Usuarios de prueba y datos de ejemplo

Según el seed de `database/mysql/brem_automotriz.sql`:

| Email | Contraseña | Rol |
| --- | --- | --- |
| `admin1@brem.cl` | `admin123` | `ADMIN` |
| `admin2@brem.cl` | `admin123` | `ADMIN` |
| `jefe@brem.cl` | `jefe123` | `JEFE` |

Login de ejemplo:

```powershell
POST http://localhost:9090/auth/login
Content-Type: application/json

{"email":"jefe@brem.cl","password":"jefe123"}
```

El token devuelto se usa como `Authorization: Bearer {{token}}` en el resto de las llamadas.

Datos de ejemplo: clientes `1` y `2`; vehículos `ABCD12`, `WXYZ34` y `BCDF56`; orden de trabajo `1` con neto `94600`, IVA `17974` y total `112574`.

## Notas

- Las órdenes de trabajo se anulan, no se borran: `brem_taller_svc` no tiene `DELETE` sobre `clientes`, `vehiculos` ni `ordenes_trabajo`.
- No se usan configuraciones de Testcontainers: los tests no levantan MySQL. En auth y taller, los controllers se prueban con `@WebMvcTest` y MockMvc (mocks con `@MockitoBean`) y los servicios con `@ExtendWith(MockitoExtension.class)` y `@Mock`; el gateway añade un test de rutas con `@SpringBootTest(webEnvironment = RANDOM_PORT)`.
- La cuenta `brem_ordenes_svc` la crea el SQL pero el servicio no la usa: `taller-service` se conecta con `brem_taller_svc`.
