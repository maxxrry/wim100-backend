# wim100-backend

Backend Spring Boot del caso WIM 100 (DSY1105, EP4/EP5, Duoc UC). Proyecto académico en pareja.
La guía completa está en `docs/guia-backend.md`. **Sigue la guía paso a paso; no inventes otra arquitectura.**

## Stack
- Spring Boot 3.x, Java 17, Maven, MySQL 8
- Dependencias: Spring Web, Spring Data JPA, MySQL Driver, Validation, Lombok, spring-security-crypto (solo BCrypt)
- Paquete base: `cl.duoc.wim100`

## Reglas
- NO agregar `spring-boot-starter-security`, JWT ni dependencias que la guía no pida.
- Estructura de paquetes: model, repository, dto, service, controller, exception, config (Paso 5).
- Enums `EstadoPostulante` y `Rol` con nombres idénticos a la app Kotlin (viajan como texto en el JSON).
- Contraseñas siempre con BCrypt, nunca en texto plano ni en respuestas.
- Todos los datos de prueba son ficticios.
- Las áreas del DataLoader deben coincidir exactamente con `Candidata.AREAS` de la app.
- Cambios pequeños: un paso de la guía por tarea, compilar y verificar antes de seguir.
- Antes de hacer `git push` o borrar archivos, pide confirmación.

## Comandos (Windows / PowerShell)
- Compilar: `.\mvnw.cmd -q compile`
- Ejecutar: `.\mvnw.cmd spring-boot:run`
- Tests: `.\mvnw.cmd test`

## Endpoints
| Método | Ruta | Respuestas |
|---|---|---|
| GET | /api/postulantes | 200 |
| GET | /api/postulantes/{id} | 200 · 404 |
| POST | /api/postulantes | 201 · 400 · 409 |
| PUT | /api/postulantes/{id} | 200 · 400 · 404 · 409 |
| PATCH | /api/postulantes/{id}/estado | 200 · 404 |
| DELETE | /api/postulantes/{id} | 204 · 404 |
| POST | /api/auth/login | 200 · 401 |
