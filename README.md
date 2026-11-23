# Brem Automotriz - Backend

**Proyecto:** Brem Automotriz, gestión de órdenes de trabajo para un taller automotriz de Maipú.

**Integrantes:** Yuliana Calvucura y Catalina Salinas.

**Asignatura:** DSY1105 Desarrollo de Aplicaciones Móviles, sección DESARROLLO DE APLICACIONES MOVILES_012D (Duoc UC). Avance final de la Experiencia 3, preparación del Examen Final Transversal (EFT).

**Repositorios:**

- Backend (este repositorio): https://github.com/Yulianacalvucura/Proyecto-Backend-BremAutomotriz-
- App Android: https://github.com/CataSalinas/Proyecto-BremAutomotriz-

Este backend sirve a la app Android de Brem Automotriz. La problemática se resume a continuación.

## Contexto y problemática

Brem Automotriz es un taller automotriz de Maipú que se gestiona con documentación física y planillas Excel. Funciona con solo dos computadores, a los que únicamente ingresan los socios fundadores. El jefe de taller, que se hace cargo del taller por las mañanas, no tiene acceso y debe anotar todo en una pizarra, por lo que se pierde información importante.

Problemas identificados:

- **Registros incompletos:** los datos de ingreso y retiro quedan con faltantes y es difícil buscar historiales.
- **Sin respaldo visual:** no hay evidencia del estado inicial del vehículo, lo que genera disputas por rayas o abolladuras previas.
- **Cobro manual:** los montos netos, IVA y totales se calculan a mano, con riesgo de errores.

La aplicación permite al jefe de taller y a los socios gestionar las órdenes de trabajo desde el celular, con búsqueda de historiales, registro de evidencias y montos calculados por el servidor.

Backend de Brem Automotriz con **Spring Boot 4.2.0 / Java 21** y una única base de datos **PostgreSQL 16 compartida** (`brem_automotriz`).

> Hay dos formas de ejecutar el proyecto: **local** (PostgreSQL 16 + Maven) y **despliegue en AWS** (Docker Compose + PostgreSQL 16). Ver la sección "Despliegue en AWS (EC2)".

El esquema lo crea el script `database/postgres/brem_automotriz.sql` (ejecutado a mano en local y montado en `/docker-entrypoint-initdb.d` en Docker); no hay Flyway, Liquibase ni DDL de Hibernate (`spring.jpa.hibernate.ddl-auto=none`).

## Servicios

| Servicio | Puerto | Contenedor Docker | Base de datos / responsabilidad |
| --- | --- | --- | --- |
| `Backend/auth-usuarios-service` | **8091** | `brem-auth` | Solo la tabla `usuarios`. Genera el JWT. |
| `Backend/taller-service` | **8081** | `brem-taller` | `clientes`, `vehiculos`, `ordenes_trabajo`, `lineas_ot`, `evidencias_ot`, `historial_estados` y la vista `vista_ordenes`. |
| `Backend/api-gateway` | **9090** | `brem-gateway` | Gateway MVC (servlet, no WebFlux). Valida el JWT e inyecta los headers `X-User-*`. No usa base de datos. |
| `db` (PostgreSQL 16) | 5432 (solo red interna) | `brem-db` | Base de datos `brem_automotriz`. |

Rutas del gateway (`spring.cloud.gateway.server.webmvc.*`), por orden de precedencia:

- `/auth/swagger-ui.html`, `/auth/swagger-ui/**`, `/auth/v3/api-docs`, `/auth/v3/api-docs/**` → `http://localhost:8091`
- `/auth/**` → `http://localhost:8091`
- `/taller/swagger-ui.html`, `/taller/swagger-ui/**`, `/taller/v3/api-docs`, `/taller/v3/api-docs/**` → `http://localhost:8081`
- `/clientes/**`, `/vehiculos/**`, `/ordenes/**`, `/taller/**` → `http://localhost:8081`
- Documentación agrupada del gateway: ver la sección **Documentación (Swagger)** más abajo.

En local se usan los defaults `http://localhost:8091` y `http://localhost:8081`; Docker Compose los sobrescribe con `http://auth:8091` y `http://taller:8081`.

## Variables de entorno (ejecución local)

Ninguna contraseña ni secreto real se guarda en el repositorio: todo se lee de variables de entorno.

| Variable | Uso |
| --- | --- |
| `DB_URL` | Opcional. URL JDBC de PostgreSQL. Default: `jdbc:postgresql://localhost:5432/brem_automotriz`. |
| `DB_USER` | Usuario de PostgreSQL. Distinto por servicio: `brem_usuarios_svc` en auth, `brem_taller_svc` en taller. |
| `DB_PASSWORD` | Contraseña de ese usuario. Distinta por servicio: la que definiste al crear cada rol con `db/postgres/usuarios_servicio.sql`. No aplica al gateway (no usa BD). |
| `JWT_SECRET` | Secreto para firmar/validar JWT. **El mismo valor en api-gateway y auth-usuarios-service**, con **mínimo 32 caracteres**. No se usa en taller-service. |
| `AUTH_SERVICE_URL` | Opcional. URL base de auth-usuarios-service usada por el gateway. Default: `http://localhost:8091`. |
| `TALLER_SERVICE_URL` | Opcional. URL base de taller-service usada por el gateway. Default: `http://localhost:8081`. |
| `CORS_ORIGINS` | Opcional. Orígenes permitidos, separados por coma; se usa en los tres servicios. Default: `http://localhost:*,http://127.0.0.1:*`. |
| `APP_PUBLIC_URL` | Opcional. URL pública que aparece en Swagger; se usa en auth y taller. Default: `http://localhost:9090`. |

Ejemplo (PowerShell, sesión de desarrollo):

```powershell
# auth-usuarios-service
$env:DB_USER = "brem_usuarios_svc"
$env:DB_PASSWORD = "<contraseña definida para brem_usuarios_svc>"
$env:JWT_SECRET = "<secreto de al menos 32 caracteres>"

# taller-service (en otra terminal, con la contraseña de su propia cuenta)
$env:DB_USER = "brem_taller_svc"
$env:DB_PASSWORD = "<contraseña definida para brem_taller_svc>"

# api-gateway (sin BD; el MISMO JWT_SECRET que auth)
$env:JWT_SECRET = "<secreto de al menos 32 caracteres>"

# Opcional en auth y taller, si PostgreSQL no está en localhost:5432:
# $env:DB_URL = "jdbc:postgresql://localhost:5432/brem_automotriz"

# Opcional en los tres servicios: CORS_ORIGINS (default http://localhost:*,http://127.0.0.1:*)
# Opcional en auth y taller: APP_PUBLIC_URL (default http://localhost:9090)
```

## Orden para levantar el proyecto (ejecución local)

1. Crear la base de datos con un usuario administrador (`postgres`):

   ```powershell
   psql -U postgres -c "CREATE DATABASE brem_automotriz;"
   ```

2. Ejecutar el script del esquema:

   ```powershell
   psql -U postgres -d brem_automotriz -f database\postgres\brem_automotriz.sql
   ```

   Ese script crea las tablas, la vista `vista_ordenes` y los datos de prueba. Los IDs son `BIGINT GENERATED BY DEFAULT AS IDENTITY` y los ENUM se sustituyeron por `VARCHAR(20)` con `CHECK`, para compatibilidad con Hibernate.

3. Ejecutar el script de los roles (mínimo privilegio):

   ```powershell
   psql -U postgres -d brem_automotriz -v usuarios_pass="<contraseña>" -v taller_pass="<contraseña>" -f db\postgres\usuarios_servicio.sql
   ```

   Crea solo `brem_usuarios_svc` (SELECT/INSERT/UPDATE sobre `usuarios`) y `brem_taller_svc` (permisos sobre las tablas de taller y la vista). Guarda esas contraseñas para usarlas como `DB_PASSWORD` en cada servicio.

4. Levantar los servicios en este orden: **auth-usuarios-service (8091)** → **taller-service (8081)** → **api-gateway (9090)**.

   ```powershell
   # ejecutado dentro de cada carpeta Backend/<módulo>
   .\mvnw.cmd spring-boot:run
   ```

## Pruebas

Ejecutadas en Windows con PowerShell y Java 21.0.11 (verificado con `java -version`), desde la carpeta de cada módulo:

```powershell
cd Backend\auth-usuarios-service
.\mvnw.cmd test
cd ..\taller-service
.\mvnw.cmd test
cd ..\api-gateway
.\mvnw.cmd test
```

Las tres ejecuciones terminaron con código de salida `0`. Los números se contaron de `target\surefire-reports\TEST-*.xml` al finalizar cada ejecución.

Tipos de pruebas por servicio:

- **auth-usuarios-service**: `AuthControllerTest` usa `@WebMvcTest` + MockMvc con `@MockitoBean`; `AuthServiceTest` usa `@ExtendWith(MockitoExtension.class)` con `@Mock`; `JwtServiceTest` y `BCryptHashVerificationTest` son pruebas unitarias JUnit sin contexto Spring.
- **taller-service**: seis `*ControllerTest` usan `@WebMvcTest` + MockMvc con `@MockitoBean` e importan `SecurityConfig` y `GlobalExceptionHandler`; tres `*ServiceTest` usan `@ExtendWith(MockitoExtension.class)` con `@Mock`.
- **api-gateway**: `ApiGatewayRoutingIntegrationTest` usa `@SpringBootTest(webEnvironment = RANDOM_PORT)`; `JwtAuthenticationFilterTest` y `CorsConfigTest` son pruebas unitarias JUnit sin contexto Spring.

| Módulo | Clase | Tests | Fallos | Errores | Omitidos |
| --- | --- | ---: | ---: | ---: | ---: |
| auth-usuarios-service | `cl.brem.authusuarios.controller.AuthControllerTest` | 6 | 0 | 0 | 0 |
| auth-usuarios-service | `cl.brem.authusuarios.security.BCryptHashVerificationTest` | 2 | 0 | 0 | 0 |
| auth-usuarios-service | `cl.brem.authusuarios.service.AuthServiceTest` | 10 | 0 | 0 | 0 |
| auth-usuarios-service | `cl.brem.authusuarios.service.JwtServiceTest` | 3 | 0 | 0 | 0 |
| **auth-usuarios-service** | **Subtotal** | **21** | **0** | **0** | **0** |
| taller-service | `cl.brem.taller.controller.ClienteControllerTest` | 9 | 0 | 0 | 0 |
| taller-service | `cl.brem.taller.controller.EvidenciaControllerTest` | 7 | 0 | 0 | 0 |
| taller-service | `cl.brem.taller.controller.HistorialControllerTest` | 4 | 0 | 0 | 0 |
| taller-service | `cl.brem.taller.controller.LineaControllerTest` | 9 | 0 | 0 | 0 |
| taller-service | `cl.brem.taller.controller.OrdenControllerTest` | 14 | 0 | 0 | 0 |
| taller-service | `cl.brem.taller.controller.VehiculoControllerTest` | 12 | 0 | 0 | 0 |
| taller-service | `cl.brem.taller.service.ClienteServiceTest` | 9 | 0 | 0 | 0 |
| taller-service | `cl.brem.taller.service.OrdenTrabajoServiceTest` | 36 | 0 | 0 | 0 |
| taller-service | `cl.brem.taller.service.VehiculoServiceTest` | 13 | 0 | 0 | 0 |
| **taller-service** | **Subtotal** | **113** | **0** | **0** | **0** |
| api-gateway | `cl.brem.apigateway.ApiGatewayRoutingIntegrationTest` | 1 | 0 | 0 | 0 |
| api-gateway | `cl.brem.apigateway.config.CorsConfigTest` | 1 | 0 | 0 | 0 |
| api-gateway | `cl.brem.apigateway.filter.JwtAuthenticationFilterTest` | 14 | 0 | 0 | 0 |
| **api-gateway** | **Subtotal** | **16** | **0** | **0** | **0** |
| **TOTAL** | | **150** | **0** | **0** | **0** |

No se usan Testcontainers: ninguna prueba levanta PostgreSQL.

## Despliegue en AWS (EC2)

El backend corre en una instancia EC2 con Docker Compose. Se levantan cuatro contenedores: `brem-db` (PostgreSQL 16, servicio `db`), `brem-auth` (servicio `auth`), `brem-taller` (servicio `taller`) y `brem-gateway` (servicio `gateway`).

```text
Internet → :9090 (API Gateway) → auth (8091) / taller (8081) → PostgreSQL (5432)
```

Solo el puerto `9090` se expone para la aplicación (el `22` se abre únicamente para administración por SSH); `auth`, `taller` y la base de datos quedan dentro de la red de Docker.

### Datos del despliegue

| Dato | Valor |
| --- | --- |
| Región | `us-east-1` (Norte de Virginia) |
| Instancia | `Brem-Automotriz` (`t3.medium`, Ubuntu 24.04) |
| IP elástica | `52.6.72.225` |
| URL pública | `http://52.6.72.225:9090` |
| Swagger | `http://52.6.72.225:9090/swagger-ui.html` |

### Pasos para desplegar

1. Crear la instancia EC2 (Ubuntu 24.04) y asociarle una IP elástica (EC2 → Direcciones IP elásticas → Asociar).

2. Abrir puertos en el Security Group: **22** (SSH) y **9090** (gateway). No abrir `5432`, `8081` ni `8091`.

3. Conectarse por SSH:

```bash
ssh -i "bremkey.pem" ubuntu@52.6.72.225
```

4. Instalar Docker y Git:

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y docker.io docker-compose-v2 git
sudo usermod -aG docker $USER && newgrp docker
```

5. Clonar el repositorio:

```bash
git clone https://github.com/Yulianacalvucura/Proyecto-Backend-BremAutomotriz-.git
cd Proyecto-Backend-BremAutomotriz-
git checkout main
```

6. Crear el archivo `.env` (no se sube a GitHub; las claves se generan al azar):

```bash
cat > .env <<EOF
POSTGRES_PASSWORD=$(openssl rand -hex 16)
DB_PASSWORD_USUARIOS=$(openssl rand -hex 16)
DB_PASSWORD_TALLER=$(openssl rand -hex 16)
JWT_SECRET=$(openssl rand -hex 32)
APP_PUBLIC_URL=http://52.6.72.225:9090
CORS_ORIGINS=http://localhost:*,http://52.6.72.225:*
EOF
```

7. Levantar los servicios:

```bash
docker compose up -d --build
docker ps
```

### Variables de entorno (despliegue en AWS)

| Variable | Uso |
| --- | --- |
| `POSTGRES_PASSWORD` | Contraseña del administrador de PostgreSQL. |
| `DB_PASSWORD_USUARIOS` | Contraseña de la base del servicio auth. |
| `DB_PASSWORD_TALLER` | Contraseña de la base del servicio taller. |
| `JWT_SECRET` | Clave para firmar los tokens (mínimo 32 caracteres). |
| `APP_PUBLIC_URL` | URL pública que aparece en Swagger. |
| `CORS_ORIGINS` | Orígenes permitidos para el navegador, separados por coma. |

La IP no está escrita en el código: se lee desde estas variables, por eso se cambia editando solo el `.env`.

### Verificar que funciona

1. Abrir `http://52.6.72.225:9090/swagger-ui.html`.
2. Hacer `POST /auth/login` con un usuario de prueba (los del README, sección **Usuarios de prueba y datos de ejemplo**) y comprobar que responde `200` con un token.
3. Enviar ese token en el header `Authorization: Bearer <token>` para consultar `GET /clientes`.

### Actualizar el despliegue

```bash
cd Proyecto-Backend-BremAutomotriz-
git pull
docker compose up -d --build
```

### Notas

- La IP elástica se mantiene aunque se apague y encienda la instancia.
- En AWS Academy el laboratorio se apaga solo. Al reiniciarlo, revisar con `docker ps` que los contenedores sigan arriba (si no, `docker compose up -d`).

## Endpoints

Todas las rutas listadas están expuestas por el gateway en `http://localhost:9090`. Son públicos `POST /auth/login`, el Swagger agrupado del gateway y la documentación de auth (`/auth/swagger-ui.html`, `/auth/v3/api-docs`); `/taller/v3/api-docs` también figura en `RUTAS_PUBLICAS` del filtro, pero `/taller/swagger-ui.html` **no**: a través del gateway requiere `Authorization: Bearer <token>`. El resto de rutas exige token.

Los permisos se aplican por servicio: en `auth-usuarios-service`, todo `/auth/**` excepto `/login` exige authority `ADMIN`; en `taller-service`, los verbos de las tablas siguientes exigen `ADMIN` o `JEFE`. Los errores de `auth-usuarios-service` y `taller-service` responden `{status, error, mensaje, timestamp}` (401/403/404/400/409/500); el 401 del gateway responde `{status, error, mensaje}`, sin `timestamp`.

### Autenticación y usuarios

| Método | Ruta | Descripción | Acceso |
| --- | --- | --- | --- |
| `POST` | `/auth/login` | Login; devuelve el JWT. | Público |
| `POST` | `/auth/registrar` | Alta de usuario. | Solo `ADMIN` |
| `GET` | `/auth/usuarios` | Listado de usuarios. | Solo `ADMIN` |
| `PATCH` | `/auth/usuarios/{id}/desactivar` | Desactiva un usuario. | Solo `ADMIN` |
| `PATCH` | `/auth/usuarios/{id}/activar` | Reactiva un usuario. | Solo `ADMIN` |

### Clientes

| Método | Ruta | Descripción | Acceso |
| --- | --- | --- | --- |
| `GET` | `/clientes` | Listado. | `ADMIN` o `JEFE` |
| `GET` | `/clientes/{id}` | Detalle. | `ADMIN` o `JEFE` |
| `POST` | `/clientes` | Alta. | `ADMIN` o `JEFE` |
| `PUT` | `/clientes/{id}` | Edición. | `ADMIN` o `JEFE` |

### Vehículos

| Método | Ruta | Descripción | Acceso |
| --- | --- | --- | --- |
| `GET` | `/vehiculos` | Listado. | `ADMIN` o `JEFE` |
| `GET` | `/vehiculos/{id}` | Detalle. | `ADMIN` o `JEFE` |
| `GET` | `/vehiculos/cliente/{clienteId}` | Vehículos de un cliente. | `ADMIN` o `JEFE` |
| `GET` | `/vehiculos/patente/{patente}` | Búsqueda por patente. | `ADMIN` o `JEFE` |
| `POST` | `/vehiculos` | Alta. | `ADMIN` o `JEFE` |
| `PUT` | `/vehiculos/{id}` | Edición. | `ADMIN` o `JEFE` |

### Órdenes de trabajo

| Método | Ruta | Descripción | Acceso |
| --- | --- | --- | --- |
| `GET` | `/ordenes` | Listado (tabla `ordenes_trabajo`). | `ADMIN` o `JEFE` |
| `GET` | `/ordenes/{id}` | Detalle. | `ADMIN` o `JEFE` |
| `GET` | `/ordenes/buscar?q=` | Búsqueda (vista `vista_ordenes`). | `ADMIN` o `JEFE` |
| `POST` | `/ordenes` | Alta; requiere `X-User-Id`. | `ADMIN` o `JEFE` |
| `PUT` | `/ordenes/{id}` | Edición. | `ADMIN` o `JEFE` |
| `PATCH` | `/ordenes/{id}/estado` | Cambio de estado. | `ADMIN` o `JEFE` |

### Líneas, evidencias e historial

| Método | Ruta | Descripción | Acceso |
| --- | --- | --- | --- |
| `GET` | `/ordenes/{ordenId}/lineas` | Listado de líneas. | `ADMIN` o `JEFE` |
| `POST` | `/ordenes/{ordenId}/lineas` | Alta de línea. | `ADMIN` o `JEFE` |
| `PUT` | `/ordenes/{ordenId}/lineas/{lineaId}` | Edición de línea. | `ADMIN` o `JEFE` |
| `DELETE` | `/ordenes/{ordenId}/lineas/{lineaId}` | Borrado de línea (único `DELETE`). | `ADMIN` o `JEFE` |
| `GET` | `/ordenes/{ordenId}/evidencias` | Listado de evidencias. | `ADMIN` o `JEFE` |
| `POST` | `/ordenes/{ordenId}/evidencias` | Alta de evidencia. | `ADMIN` o `JEFE` |
| `GET` | `/ordenes/{ordenId}/historial` | Historial de cambios de estado. | `ADMIN` o `JEFE` |

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

Según `JwtAuthenticationFilter.RUTAS_PUBLICAS`, `/taller/swagger-ui.html` no es pública a través del gateway: envía `Authorization: Bearer <token>` o usa el puerto directo `http://localhost:8081/taller/swagger-ui.html`, donde Swagger está permitido por el `SecurityConfig` del servicio.

También disponibles en los puertos directos: `http://localhost:8091/auth/swagger-ui.html` y `http://localhost:8081/taller/swagger-ui.html`.

## Usuarios de prueba y datos de ejemplo

Según el seed de `database/postgres/brem_automotriz.sql`:

| Email | Contraseña | Rol |
| --- | --- | --- |
| `admin1@brem.cl` | `admin123` | `ADMIN` |
| `admin2@brem.cl` | `admin123` | `ADMIN` |
| `jefe@brem.cl` | `jefe123` | `JEFE` |

Estas credenciales son solo de prueba para la evaluación; deben cambiarse antes de cualquier uso real.

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
- Los tipos y resultados de las pruebas están en la sección **Pruebas**.
- El script PostgreSQL crea solo los roles `brem_usuarios_svc` y `brem_taller_svc`: `taller-service` se conecta con `brem_taller_svc`.
- En el repositorio queda un script histórico `database/mysql/brem_automotriz.sql`, pero ningún módulo usa MySQL: los tres `pom.xml` y la configuración usan PostgreSQL (`org.postgresql.Driver`).