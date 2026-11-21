# Brem Automotriz - Backend

Backend de Brem Automotriz con **Spring Boot 4.2.0 / Java 21** y una única base de datos **MySQL 8 compartida** (`brem_automotriz`).

El esquema lo crea exclusivamente el script SQL ejecutado a mano: no hay Flyway, Liquibase ni DDL de Hibernate (`spring.jpa.hibernate.ddl-auto=none`).

## Servicios

| Servicio | Puerto | Base de datos / responsabilidad |
| --- | --- | --- |
| `Backend/auth-usuarios-service` | **8091** | Solo la tabla `usuarios`. Genera el JWT. |
| `Backend/taller-service` | **8081** | `clientes`, `vehiculos`, `ordenes_trabajo`, `lineas_ot`, `evidencias_ot`, `historial_estados` y la vista `vista_ordenes`. |
| `Backend/api-gateway` | **9090** | Gateway MVC (servlet, no WebFlux). En fases posteriores validará el JWT y enviará los headers `X-User-*`. No usa base de datos. |

Rutas del gateway (`spring.cloud.gateway.server.webmvc.*`):

- `/auth/**` → `http://localhost:8091`
- `/clientes/**`, `/vehiculos/**`, `/ordenes/**` → `http://localhost:8081`
- Documentación: `/auth/swagger-ui.html`, `/auth/v3/api-docs`, `/taller/swagger-ui.html`, `/taller/v3/api-docs`

## Variables de entorno

Ninguna contraseña ni secreto real se guarda en el repositorio: todo se lee de variables de entorno.

| Variable | Uso |
| --- | --- |
| `DB_USER` | Usuario de MySQL. Distinto por servicio: `brem_usuarios_svc` en auth, `brem_taller_svc` en taller. |
| `DB_PASSWORD` | Contraseña de ese usuario. Distinta por servicio: la que imprimió MySQL para cada cuenta. No aplica al gateway (no usa BD). |
| `JWT_SECRET` | Secreto para firmar/validar JWT. **El mismo valor en api-gateway y auth-usuarios-service**, con **mínimo 32 caracteres**. No se usa en taller-service. |

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
   # desde cada carpeta, con Maven en el PATH
   mvn spring-boot:run
   ```

## Notas

- Las órdenes de trabajo se anulan, no se borran: `brem_taller_svc` no tiene `DELETE` sobre `clientes`, `vehiculos` ni `ordenes_trabajo`.
- No se conservan configuraciones de Testcontainers ni tests `@SpringBootTest`: se reemplazarán por tests unitarios con Mockito en fases siguientes.
