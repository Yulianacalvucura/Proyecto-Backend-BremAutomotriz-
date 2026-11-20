# Proyecto-Backend-BremAutomotriz-

Backend de Brem Automotriz: servicios Spring Boot (Java 21) sobre una base de datos **MySQL 8 compartida**.

## Estructura

| Módulo | Base de datos |
| --- | --- |
| `Backend/api-gateway` | No usa BD |
| `Backend/auth-usuarios-service` | `usuarios` |
| `Backend/taller-service` | `clientes`, `vehiculos`, `ordenes_trabajo`, `lineas_ot`, `evidencias_ot`, `historial_estados`, `vista_ordenes` |

## Base de datos

El esquema, los usuarios de MySQL y los datos de prueba están en [`database/mysql/brem_automotriz.sql`](database/mysql/brem_automotriz.sql).

Es un **script de inicialización**: lo ejecuta una vez un administrador y la aplicación no gestiona el esquema.

Requisito: MySQL **8.0.18 o superior** (usa `CHECK`, `REGEXP_LIKE` e `IDENTIFIED BY RANDOM PASSWORD`).

```powershell
mysql -u root -p < database\mysql\brem_automotriz.sql
```

El script crea la base `brem_automotriz`, las tablas, la vista `vista_ordenes`, tres cuentas con mínimo privilegio y datos de prueba. MySQL muestra al ejecutarlo las contraseñas aleatorias generadas para esas cuentas: guárdalas en tu gestor de secretos. **No las subas al repositorio.**

Decisiones tomadas:

- **Sin Flyway**: al compartir una sola BD, cada servicio tendría su propia tabla de historial de migraciones y se pisarían. El script SQL es la única fuente del esquema.
- **Sin DDL de Hibernate** (`spring.jpa.hibernate.ddl-auto=none`): la app nunca altera un esquema compartido.
- Las contraseñas de los usuarios de prueba se guardan como hash BCrypt, nunca en texto plano.

## Configuración de los servicios

La URL de MySQL se arma con:

| Variable | Valor por defecto |
| --- | --- |
| `BREM_DB_HOST` | `localhost` |
| `BREM_DB_PORT` | `3306` |
| `BREM_DB_NAME` | `brem_automotriz` |

Las credenciales se leen de las variables estándar de Spring `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`. No hay credenciales escritas en el repositorio ni valores por defecto para la contraseña.

Ejemplo para `auth-usuarios-service` (PowerShell):

```powershell
$env:SPRING_DATASOURCE_USERNAME = "brem_usuarios_svc"
$env:SPRING_DATASOURCE_PASSWORD = "<contraseña entregada por MySQL>"
Set-Location Backend\auth-usuarios-service
.\mvnw spring-boot:run
```

Si el servidor usa SSL o se conecta desde otro host, sobrescribe la URL completa con `SPRING_DATASOURCE_URL` en lugar de las variables `BREM_DB_*`.

## Pendiente de la siguiente iteración

El script define cuentas separadas para clientes y órdenes (`brem_clientes_svc` y `brem_ordenes_svc`), pero hoy `taller-service` cubre ambos dominios con una sola conexión. Hasta separar ese servicio, configura `SPRING_DATASOURCE_USERNAME` con una cuenta que tenga ambos conjuntos de permisos (o ajusta los `GRANT` del script).
