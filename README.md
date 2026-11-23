# wim100-backend

Backend Spring Boot del caso **WIM 100 – DYM Cyber** (DSY1105, EP4/EP5, Duoc UC).
CRUD de postulantes y login de evaluadora y dirección con contraseñas cifradas con BCrypt.
Lo consume la app Android **WIM100** mediante Retrofit.

> **Datos ficticios.** Todas las personas, correos (`@ejemplo.cl`), teléfonos y perfiles son inventados.

## Requisitos

- JDK 17 o superior (sirve el JDK 21 de Android Studio)
- MySQL 8 corriendo en `localhost:3306`
- No hace falta instalar Maven: el proyecto trae el wrapper (`mvnw.cmd`)
- Postman, para probar los endpoints

Stack: Spring Boot 3.5, Java 17, Spring Web, Spring Data JPA, Validation, Lombok, MySQL Driver y
`spring-security-crypto` (solo BCrypt, **sin** `spring-boot-starter-security`).

## Base de datos

No hace falta crear tablas: JPA las crea solas (`ddl-auto=update`), y la URL de conexión trae
`createDatabaseIfNotExist=true`, así que la base `wim100` también se crea sola al primer arranque.
Si prefieres crearla a mano en Workbench:

```sql
CREATE DATABASE IF NOT EXISTS wim100 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Usuario y contraseña de MySQL

En `src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD:root}
```

El usuario es `root`. La contraseña se lee de la variable de entorno `DB_PASSWORD`; si no existe, se
usa `root`. Así cada integrante usa la suya sin subirla a GitHub. Si tu usuario no es `root`, cambia
`spring.datasource.username`.

## Ejecutar

En PowerShell, desde la carpeta del proyecto:

```powershell
$env:DB_PASSWORD = "tu_contraseña_de_mysql"
.\mvnw.cmd spring-boot:run
```

O desde IntelliJ: Run sobre `Wim100BackendApplication` (agrega `DB_PASSWORD` en
*Run Configuration › Environment variables* y activa *Annotation Processing* para Lombok).

En la consola debe aparecer `Tomcat started on port 8080`. Al arrancar, `DataLoader` crea los
usuarios y 3 postulantes de prueba si las tablas están vacías.

Otros comandos:

```powershell
.\mvnw.cmd -q compile
```

```powershell
.\mvnw.cmd test
```

## Endpoints

Base: `http://localhost:8080` (desde el emulador de Android: `http://10.0.2.2:8080/api/`).

| Método | Ruta | Uso | Respuestas |
|---|---|---|---|
| GET | /api/postulantes | Listar postulantes | 200 |
| GET | /api/postulantes/{id} | Ver un postulante | 200 · 404 |
| POST | /api/postulantes | Registrar | 201 · 400 · 409 (correo repetido) |
| PUT | /api/postulantes/{id} | Editar | 200 · 400 · 404 · 409 |
| PATCH | /api/postulantes/{id}/estado | Cambiar estado | 200 · 404 |
| DELETE | /api/postulantes/{id} | Eliminar | 204 · 404 |
| POST | /api/auth/login | Iniciar sesión | 200 · 401 |

Valores que deben coincidir con la app WIM100 (viajan como texto en el JSON):

- **estado**: `RECIBIDA`, `EN_REVISION`, `EVALUADA`, `SELECCIONADA`, `NO_SELECCIONADA`
- **rol**: `EVALUADORA`, `DIRECCION`
- **area**: `Pentesting`, `Operaciones SOC`, `Cumplimiento normativo`, `Seguridad en la nube`,
  `Análisis de malware`, `Concientización y formación` (igual que `Candidata.ESPECIALIDADES`)

Ejemplo de cuerpo para POST/PUT `/api/postulantes`:

```json
{
  "nombre": "Antonia Pérez Lillo",
  "correo": "antonia.perez@ejemplo.cl",
  "telefono": "+56 9 1111 2299",
  "area": "Análisis de malware",
  "aniosExperiencia": 6,
  "resumenExperiencia": "Analista forense en un equipo ficticio de respuesta a incidentes.",
  "linkedinUrl": "https://www.linkedin.com/in/antonia-perez-demo",
  "cvNombre": "CV_Antonia.pdf",
  "registradaPor": "Camila Rojas Demo"
}
```

Los errores responden `{ "status": 400, "mensaje": "Datos inválidos", "errores": { "campo": "mensaje" } }`.

## Usuarios de prueba

| Usuario | Contraseña | Rol |
|---|---|---|
| evaluadora1 | wim2026 | EVALUADORA |
| evaluadora2 | wim2026 | EVALUADORA |
| direccion | wim2026 | DIRECCION |

En la base solo se guarda el hash BCrypt; la respuesta del login nunca incluye la contraseña.

## Pruebas con Postman

Importa `postman/wim100.postman_collection.json`. Trae las 9 pruebas del Paso 15 con sus
validaciones automáticas. Ejecútalas en orden (Run collection) sobre una base recién cargada: la
prueba 4 crea el postulante con id 4, que usan las pruebas 7, 8 y 9. Para repetirlas, borra la base
(`DROP DATABASE wim100;`) y vuelve a arrancar el backend.

## Estructura

```
cl.duoc.wim100
├── Wim100BackendApplication.java
├── model/        Entidades JPA y enums (Postulante, Usuario, EstadoPostulante, Rol)
├── repository/   Interfaces JpaRepository
├── dto/          Objetos de entrada y salida de la API (con validaciones)
├── service/      Lógica de negocio (correo único, cambio de estado, login)
├── controller/   Endpoints REST
├── exception/    Errores propios y manejador global
└── config/       Bean de BCrypt y carga de datos de prueba
```
