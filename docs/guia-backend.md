**GUÍA DOCENTE**

**Guía paso a paso: backend Spring Boot y conexión con la app Kotlin WIM 100**

DSY1105 Desarrollo de Aplicaciones Móviles · Evaluación Parcial 4 (encargo) y 5 (defensa)

Caso WIM 100 · DYM Cyber · Duoc UC

Stack: Spring Boot 3 · Java 17 · MySQL 8 · Kotlin · Jetpack Compose · Room · Retrofit · MockK

*Todos los datos usados son ficticios, como exige el caso.*

**Contenido**

*Si el índice aparece vacío: clic derecho sobre él › Actualizar campos.*

## Antes de empezar

Esta guía parte del proyecto de referencia **Proyecto\_Kotlin\_WIM100** (Kotlin, Jetpack Compose, MVVM y Room) y lo lleva a lo que pide la EP4: un backend propio en Spring Boot con base de datos, conectado a la app mediante Retrofit, más una API externa, pruebas unitarias y APK firmado.

**Decisiones de diseño de esta guía.** Un solo microservicio Spring Boot (**wim100-backend**) con el CRUD de postulantes y el login de evaluadora y dirección. Base de datos **MySQL 8**. La app es **híbrida**: postulantes y login viven en el servidor; evaluaciones, criterios e historial siguen en Room. Room guarda además una copia local de los postulantes para que las evaluaciones (que tienen llave foránea a la candidata) sigan funcionando. No se usa JWT ni Spring Security completo.

### Qué se va a construir

| **Pieza** | **Tecnología** | **Responsabilidad** |
| --- | --- | --- |
| wim100-backend | Spring Boot 3, Java 17, MySQL 8 | CRUD de postulantes, login con contraseña cifrada (BCrypt) y datos de prueba ficticios |
| wim100-app | Kotlin, Compose, MVVM, Room, Retrofit | Pantallas, login contra el backend, CRUD remoto, evaluaciones locales, API externa |
| API externa | randomuser.me (pública, sin key) | Generar un nombre ficticio para el formulario de candidatura |

### Endpoints del backend

| **Método** | **Ruta** | **Uso** | **Respuestas** |
| --- | --- | --- | --- |
| GET | /api/postulantes | Listar postulantes | 200 |
| GET | /api/postulantes/{id} | Ver un postulante | 200 · 404 |
| POST | /api/postulantes | Registrar | 201 · 400 · 409 (correo repetido) |
| PUT | /api/postulantes/{id} | Editar | 200 · 400 · 404 · 409 |
| PATCH | /api/postulantes/{id}/estado | Cambiar estado (evaluación o resolución) | 200 · 404 |
| DELETE | /api/postulantes/{id} | Eliminar | 204 · 404 |
| POST | /api/auth/login | Iniciar sesión | 200 · 401 |

### Herramientas necesarias

* JDK 17 o superior (el JDK 21 que trae Android Studio sirve).
* IntelliJ IDEA (Community basta) para el backend y Android Studio para la app.
* MySQL Server 8 y MySQL Workbench (o XAMPP con MySQL/MariaDB).
* Postman o Insomnia para probar los endpoints.
* Git y una cuenta de GitHub. Se usan **dos repositorios**: wim100-backend y wim100-app.

# Parte 1 · Backend Spring Boot

## Paso 1 · Preparar MySQL

Instala MySQL 8 y anota el usuario y la contraseña de root. No hace falta crear tablas: JPA las crea solas. Basta con que el servidor esté corriendo. Si quieres crear la base a mano, en Workbench ejecuta:

CREATE DATABASE IF NOT EXISTS wim100 CHARACTER SET utf8mb4 COLLATE utf8mb4\_unicode\_ci;

La URL de conexión del Paso 4 incluye **createDatabaseIfNotExist=true**, así que la base también se crea sola la primera vez que arranca el backend.

## Paso 2 · Crear el proyecto en Spring Initializr

Entra a start.spring.io (o en IntelliJ: File › New › Project › Spring Boot) y completa:

| **Campo** | **Valor** |
| --- | --- |
| Project | Maven |
| Language | Java |
| Spring Boot | 3.x (la estable más reciente de la serie 3) |
| Group | cl.duoc |
| Artifact / Name | wim100-backend |
| Package name | cl.duoc.wim100 |
| Packaging / Java | Jar · 17 |
| Dependencies | Spring Web, Spring Data JPA, MySQL Driver, Validation, Lombok |

Descarga el .zip, descomprímelo y ábrelo en IntelliJ. Espera a que Maven termine de descargar las dependencias.

## Paso 3 · Completar el pom.xml

Spring Initializr no ofrece el encriptador de contraseñas, así que se agrega a mano dentro de **<dependencies>**. Las versiones las administra Spring Boot.

<!-- Solo el encriptador BCrypt. NO usar spring-boot-starter-security:

bloquearía todos los endpoints con un login automático (todo respondería 401). -->

<dependency>

<groupId>org.springframework.security</groupId>

<artifactId>spring-security-crypto</artifactId>

</dependency>

Después de guardar, en IntelliJ pulsa el ícono de Maven › Reload project.

Si usas IntelliJ y Lombok, activa **Settings › Build › Compiler › Annotation Processors › Enable annotation processing**. Sin eso, los getters y setters generados por Lombok no existen y el proyecto no compila.

## Paso 4 · Configurar application.properties

Reemplaza el contenido de **src/main/resources/application.properties**. Cambia usuario y contraseña por los de tu MySQL.

spring.application.name=wim100-backend

server.port=8080

# Conexión a MySQL (la base se crea sola si no existe)

spring.datasource.url=jdbc:mysql://localhost:3306/wim100?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Santiago

spring.datasource.username=root

spring.datasource.password=root

# JPA crea y actualiza las tablas a partir de las entidades

spring.jpa.hibernate.ddl-auto=update

spring.jpa.show-sql=true

spring.jpa.open-in-view=false

## Paso 5 · Estructura de paquetes

Dentro de **src/main/java/cl/duoc/wim100** crea estos paquetes. Esta es la estructura que se explica en la defensa (punto 2 de la pauta EP5):

cl.duoc.wim100

├── Wim100BackendApplication.java (ya viene creada)

├── model/ Entidades JPA y enums

├── repository/ Interfaces JpaRepository

├── dto/ Objetos de entrada y salida de la API

├── service/ Lógica de negocio

├── controller/ Endpoints REST

├── exception/ Errores propios y manejador global

└── config/ Beans de configuración y carga de datos de prueba

| **Capa** | **Qué hace** | **Ejemplo en este proyecto** |
| --- | --- | --- |
| controller | Recibe la petición HTTP, valida el JSON y responde | PostulanteController |
| service | Aplica las reglas de negocio | Correo único, cambio de estado |
| repository | Habla con la base de datos | PostulanteRepository |
| model | Representa las tablas | Postulante, Usuario |

## Paso 6 · Enums del modelo

Los nombres deben ser **idénticos** a los de la app (EstadoCandidatura y Rol), porque viajan como texto en el JSON.

// model/EstadoPostulante.java

package cl.duoc.wim100.model;

public enum EstadoPostulante {

POSTULADA, EN\_EVALUACION, EVALUADA, SELECCIONADA, NO\_SELECCIONADA

}

// model/Rol.java

package cl.duoc.wim100.model;

public enum Rol {

EVALUADORA, DIRECCION

}

## Paso 7 · Entidad Postulante

Tiene los mismos campos que la clase **Candidata** de la app, salvo **cvUri**: esa ruta apunta a un archivo dentro del teléfono y no sirve en el servidor. El servidor solo guarda el nombre del archivo.

// model/Postulante.java

package cl.duoc.wim100.model;

import jakarta.persistence.\*;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;

@Entity

@Table(name = "postulantes")

@Getter

@Setter

@NoArgsConstructor

public class Postulante {

@Id

@GeneratedValue(strategy = GenerationType.IDENTITY)

private Long id;

@Column(nullable = false, length = 60)

private String nombre;

@Column(nullable = false, unique = true, length = 120)

private String correo;

@Column(nullable = false, length = 20)

private String telefono;

@Column(nullable = false, length = 60)

private String area;

@Column(nullable = false)

private Integer aniosExperiencia;

@Column(nullable = false, length = 500)

private String resumenExperiencia;

@Column(nullable = false, length = 200)

private String linkedinUrl;

private String cvNombre;

@Enumerated(EnumType.STRING)

@Column(nullable = false, length = 20)

private EstadoPostulante estado = EstadoPostulante.POSTULADA;

/\*\* Milisegundos desde 1970, igual que System.currentTimeMillis() en la app. \*/

@Column(nullable = false)

private Long fechaRegistro;

@Column(nullable = false, length = 80)

private String registradaPor;

}

## Paso 8 · Entidad Usuario (para el login)

No tiene CRUD: los usuarios se cargan al iniciar (Paso 14). La contraseña se guarda **cifrada con BCrypt**, nunca en texto plano.

// model/Usuario.java

package cl.duoc.wim100.model;

import jakarta.persistence.\*;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;

@Entity

@Table(name = "usuarios")

@Getter

@Setter

@NoArgsConstructor

public class Usuario {

@Id

@GeneratedValue(strategy = GenerationType.IDENTITY)

private Long id;

@Column(nullable = false, unique = true, length = 40)

private String usuario;

/\*\* Hash BCrypt (60 caracteres), nunca la contraseña en texto plano. \*/

@Column(nullable = false, length = 100)

private String password;

@Column(nullable = false, length = 80)

private String nombre;

@Enumerated(EnumType.STRING)

@Column(nullable = false, length = 20)

private Rol rol;

public Usuario(String usuario, String password, String nombre, Rol rol) {

this.usuario = usuario;

this.password = password;

this.nombre = nombre;

this.rol = rol;

}

}

## Paso 9 · Repositorios

Spring Data JPA genera la consulta a partir del nombre del método. No hay que escribir SQL.

// repository/PostulanteRepository.java

package cl.duoc.wim100.repository;

import cl.duoc.wim100.model.Postulante;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostulanteRepository extends JpaRepository<Postulante, Long> {

List<Postulante> findAllByOrderByFechaRegistroDesc();

boolean existsByCorreoIgnoreCase(String correo);

boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

}

// repository/UsuarioRepository.java

package cl.duoc.wim100.repository;

import cl.duoc.wim100.model.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

Optional<Usuario> findByUsuario(String usuario);

}

## Paso 10 · DTOs con validaciones

Los DTOs definen qué JSON acepta la API. Las anotaciones de **jakarta.validation** rechazan datos inválidos con un 400 antes de llegar al servicio. Se usan records de Java 17 para escribir menos código.

// dto/PostulanteRequest.java

package cl.duoc.wim100.dto;

import jakarta.validation.constraints.\*;

public record PostulanteRequest(

@NotBlank(message = "El nombre es obligatorio")

@Size(min = 3, max = 60, message = "El nombre debe tener entre 3 y 60 caracteres")

String nombre,

@NotBlank(message = "El correo es obligatorio")

@Email(message = "Formato de correo inválido")

String correo,

@NotBlank(message = "El teléfono es obligatorio")

@Size(max = 20)

String telefono,

@NotBlank(message = "El área es obligatoria")

String area,

@NotNull(message = "Indica los años de experiencia")

@Min(value = 0, message = "No puede ser negativo")

@Max(value = 50, message = "Máximo 50 años")

Integer aniosExperiencia,

@NotBlank(message = "El resumen es obligatorio")

@Size(min = 30, max = 500, message = "El resumen debe tener entre 30 y 500 caracteres")

String resumenExperiencia,

@NotBlank(message = "El enlace al perfil es obligatorio")

@Pattern(regexp = "^https://.\*", message = "Debe comenzar con https://")

String linkedinUrl,

String cvNombre,

@NotBlank(message = "Falta quién registra la candidatura")

String registradaPor

) {}

// dto/EstadoRequest.java

package cl.duoc.wim100.dto;

import cl.duoc.wim100.model.EstadoPostulante;

import jakarta.validation.constraints.NotNull;

public record EstadoRequest(@NotNull(message = "El estado es obligatorio") EstadoPostulante estado) {}

// dto/LoginRequest.java

package cl.duoc.wim100.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

@NotBlank(message = "Ingresa tu usuario") String usuario,

@NotBlank(message = "Ingresa tu contraseña") String password

) {}

// dto/LoginResponse.java

package cl.duoc.wim100.dto;

import cl.duoc.wim100.model.Rol;

/\*\* Lo que la app recibe al iniciar sesión. Nunca incluye la contraseña. \*/

public record LoginResponse(Long id, String usuario, String nombre, Rol rol) {}

// dto/ErrorResponse.java

package cl.duoc.wim100.dto;

import java.util.Map;

public record ErrorResponse(int status, String mensaje, Map<String, String> errores) {}

Spring Boot ignora los campos del JSON que el DTO no declara (id, estado, fechaRegistro). Por eso la app puede enviar el postulante completo y el servidor solo toma lo que corresponde.

## Paso 11 · Excepciones y manejador global

Cada error de negocio tiene su excepción, y **@RestControllerAdvice** la transforma en el código HTTP correcto. Así la app puede mostrar un mensaje claro según el código que recibe.

// exception/RecursoNoEncontradoException.java

package cl.duoc.wim100.exception;

public class RecursoNoEncontradoException extends RuntimeException {

public RecursoNoEncontradoException(String mensaje) { super(mensaje); }

}

// exception/CorreoDuplicadoException.java

package cl.duoc.wim100.exception;

public class CorreoDuplicadoException extends RuntimeException {

public CorreoDuplicadoException(String mensaje) { super(mensaje); }

}

// exception/CredencialesInvalidasException.java

package cl.duoc.wim100.exception;

public class CredencialesInvalidasException extends RuntimeException {

public CredencialesInvalidasException(String mensaje) { super(mensaje); }

}

// exception/GlobalExceptionHandler.java

package cl.duoc.wim100.exception;

import cl.duoc.wim100.dto.ErrorResponse;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.MethodArgumentNotValidException;

import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;

import java.util.Map;

@RestControllerAdvice

public class GlobalExceptionHandler {

@ExceptionHandler(RecursoNoEncontradoException.class)

public ResponseEntity<ErrorResponse> noEncontrado(RecursoNoEncontradoException e) {

return respuesta(HttpStatus.NOT\_FOUND, e.getMessage(), Map.of());

}

@ExceptionHandler(CorreoDuplicadoException.class)

public ResponseEntity<ErrorResponse> duplicado(CorreoDuplicadoException e) {

return respuesta(HttpStatus.CONFLICT, e.getMessage(), Map.of());

}

@ExceptionHandler(CredencialesInvalidasException.class)

public ResponseEntity<ErrorResponse> credenciales(CredencialesInvalidasException e) {

return respuesta(HttpStatus.UNAUTHORIZED, e.getMessage(), Map.of());

}

/\*\* Errores de @Valid: devuelve un mensaje por campo. \*/

@ExceptionHandler(MethodArgumentNotValidException.class)

public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException e) {

Map<String, String> errores = new HashMap<>();

e.getBindingResult().getFieldErrors()

.forEach(f -> errores.put(f.getField(), f.getDefaultMessage()));

return respuesta(HttpStatus.BAD\_REQUEST, "Datos inválidos", errores);

}

private ResponseEntity<ErrorResponse> respuesta(HttpStatus status, String mensaje, Map<String, String> errores) {

return ResponseEntity.status(status).body(new ErrorResponse(status.value(), mensaje, errores));

}

}

## Paso 12 · Servicios (lógica de negocio)

Primero el bean del encriptador, para poder inyectarlo donde se necesite:

// config/PasswordConfig.java

package cl.duoc.wim100.config;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration

public class PasswordConfig {

@Bean

public PasswordEncoder passwordEncoder() {

return new BCryptPasswordEncoder();

}

}

// service/PostulanteService.java

package cl.duoc.wim100.service;

import cl.duoc.wim100.dto.PostulanteRequest;

import cl.duoc.wim100.exception.CorreoDuplicadoException;

import cl.duoc.wim100.exception.RecursoNoEncontradoException;

import cl.duoc.wim100.model.EstadoPostulante;

import cl.duoc.wim100.model.Postulante;

import cl.duoc.wim100.repository.PostulanteRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service

public class PostulanteService {

private final PostulanteRepository repository;

public PostulanteService(PostulanteRepository repository) {

this.repository = repository;

}

public List<Postulante> listar() {

return repository.findAllByOrderByFechaRegistroDesc();

}

public Postulante obtener(Long id) {

return repository.findById(id)

.orElseThrow(() -> new RecursoNoEncontradoException("No existe el postulante " + id));

}

public Postulante crear(PostulanteRequest req) {

// Regla de negocio: no se registra dos veces el mismo correo

if (repository.existsByCorreoIgnoreCase(req.correo().trim())) {

throw new CorreoDuplicadoException("Ya existe una candidatura con este correo");

}

Postulante p = new Postulante();

copiarDatos(req, p);

p.setEstado(EstadoPostulante.POSTULADA);

p.setFechaRegistro(System.currentTimeMillis());

p.setRegistradaPor(req.registradaPor().trim());

return repository.save(p);

}

public Postulante actualizar(Long id, PostulanteRequest req) {

Postulante p = obtener(id);

if (repository.existsByCorreoIgnoreCaseAndIdNot(req.correo().trim(), id)) {

throw new CorreoDuplicadoException("Ya existe una candidatura con este correo");

}

copiarDatos(req, p); // no cambia estado, fecha ni quién la registró

return repository.save(p);

}

public Postulante cambiarEstado(Long id, EstadoPostulante estado) {

Postulante p = obtener(id);

p.setEstado(estado);

return repository.save(p);

}

public void eliminar(Long id) {

repository.delete(obtener(id)); // lanza 404 si no existe

}

private void copiarDatos(PostulanteRequest r, Postulante p) {

p.setNombre(r.nombre().trim().replaceAll("\\s+", " "));

p.setCorreo(r.correo().trim().toLowerCase());

p.setTelefono(r.telefono().trim());

p.setArea(r.area().trim());

p.setAniosExperiencia(r.aniosExperiencia());

p.setResumenExperiencia(r.resumenExperiencia().trim());

p.setLinkedinUrl(r.linkedinUrl().trim());

p.setCvNombre(r.cvNombre());

}

}

// service/AuthService.java

package cl.duoc.wim100.service;

import cl.duoc.wim100.dto.LoginRequest;

import cl.duoc.wim100.dto.LoginResponse;

import cl.duoc.wim100.exception.CredencialesInvalidasException;

import cl.duoc.wim100.model.Usuario;

import cl.duoc.wim100.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

@Service

public class AuthService {

private final UsuarioRepository repository;

private final PasswordEncoder encoder;

public AuthService(UsuarioRepository repository, PasswordEncoder encoder) {

this.repository = repository;

this.encoder = encoder;

}

public LoginResponse login(LoginRequest req) {

Usuario u = repository.findByUsuario(req.usuario().trim().toLowerCase())

// matches() compara la contraseña escrita con el hash guardado

.filter(x -> encoder.matches(req.password(), x.getPassword()))

.orElseThrow(() -> new CredencialesInvalidasException("Usuario o contraseña incorrectos"));

return new LoginResponse(u.getId(), u.getUsuario(), u.getNombre(), u.getRol());

}

}

El mensaje de error es el mismo si el usuario no existe o si la contraseña está mal. Así no se le revela a un atacante qué usuarios existen.

## Paso 13 · Controladores REST

// controller/PostulanteController.java

package cl.duoc.wim100.controller;

import cl.duoc.wim100.dto.EstadoRequest;

import cl.duoc.wim100.dto.PostulanteRequest;

import cl.duoc.wim100.model.Postulante;

import cl.duoc.wim100.service.PostulanteService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.\*;

import java.util.List;

@RestController

@RequestMapping("/api/postulantes")

public class PostulanteController {

private final PostulanteService service;

public PostulanteController(PostulanteService service) {

this.service = service;

}

@GetMapping

public List<Postulante> listar() {

return service.listar();

}

@GetMapping("/{id}")

public Postulante obtener(@PathVariable Long id) {

return service.obtener(id);

}

@PostMapping

public ResponseEntity<Postulante> crear(@Valid @RequestBody PostulanteRequest req) {

return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));

}

@PutMapping("/{id}")

public Postulante actualizar(@PathVariable Long id, @Valid @RequestBody PostulanteRequest req) {

return service.actualizar(id, req);

}

@PatchMapping("/{id}/estado")

public Postulante cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest req) {

return service.cambiarEstado(id, req.estado());

}

@DeleteMapping("/{id}")

public ResponseEntity<Void> eliminar(@PathVariable Long id) {

service.eliminar(id);

return ResponseEntity.noContent().build();

}

}

// controller/AuthController.java

package cl.duoc.wim100.controller;

import cl.duoc.wim100.dto.LoginRequest;

import cl.duoc.wim100.dto.LoginResponse;

import cl.duoc.wim100.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.\*;

@RestController

@RequestMapping("/api/auth")

public class AuthController {

private final AuthService service;

public AuthController(AuthService service) {

this.service = service;

}

@PostMapping("/login")

public LoginResponse login(@Valid @RequestBody LoginRequest req) {

return service.login(req);

}

}

## Paso 14 · Datos de prueba ficticios

Un **CommandLineRunner** se ejecuta al arrancar. Si las tablas están vacías, crea los usuarios (con la contraseña cifrada) y algunos postulantes. Todos los datos son ficticios, como exige el caso.

// config/DataLoader.java

package cl.duoc.wim100.config;

import cl.duoc.wim100.model.EstadoPostulante;

import cl.duoc.wim100.model.Postulante;

import cl.duoc.wim100.model.Rol;

import cl.duoc.wim100.model.Usuario;

import cl.duoc.wim100.repository.PostulanteRepository;

import cl.duoc.wim100.repository.UsuarioRepository;

import org.springframework.boot.CommandLineRunner;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Component;

import java.util.List;

@Component

public class DataLoader implements CommandLineRunner {

private final UsuarioRepository usuarios;

private final PostulanteRepository postulantes;

private final PasswordEncoder encoder;

public DataLoader(UsuarioRepository usuarios, PostulanteRepository postulantes, PasswordEncoder encoder) {

this.usuarios = usuarios;

this.postulantes = postulantes;

this.encoder = encoder;

}

@Override

public void run(String... args) {

if (usuarios.count() == 0) {

usuarios.saveAll(List.of(

new Usuario("evaluadora1", encoder.encode("wim2026"), "Camila Rojas Demo", Rol.EVALUADORA),

new Usuario("evaluadora2", encoder.encode("wim2026"), "Valentina Muñoz Demo", Rol.EVALUADORA),

new Usuario("direccion", encoder.encode("wim2026"), "Dirección WIM Demo", Rol.DIRECCION)

));

}

if (postulantes.count() == 0) {

postulantes.saveAll(List.of(

nueva("Ignacia Valdés Soto", "ignacia.valdes@ejemplo.cl", "+56 9 1111 2201",

"Seguridad ofensiva (Red Team)", 9,

"Pentester líder en una consultora ficticia. Coordina ejercicios de Red Team.",

"https://www.linkedin.com/in/ignacia-valdes-demo"),

nueva("Martina Herrera Lagos", "martina.herrera@ejemplo.cl", "+56 9 1111 2202",

"Seguridad defensiva (Blue Team)", 7,

"Jefa de un SOC ficticio. Diseñó playbooks de detección de incidentes.",

"https://www.linkedin.com/in/martina-herrera-demo"),

nueva("Fernanda Castillo Rivas", "fernanda.castillo@ejemplo.cl", "+56 9 1111 2203",

"Gobierno, riesgo y cumplimiento", 12,

"Especialista en ISO 27001 en una empresa ficticia del sector financiero.",

"https://www.linkedin.com/in/fernanda-castillo-demo")

));

}

}

private Postulante nueva(String nombre, String correo, String telefono, String area,

int anios, String resumen, String linkedin) {

Postulante p = new Postulante();

p.setNombre(nombre);

p.setCorreo(correo);

p.setTelefono(telefono);

p.setArea(area);

p.setAniosExperiencia(anios);

p.setResumenExperiencia(resumen);

p.setLinkedinUrl(linkedin);

p.setCvNombre("CV\_" + nombre.split(" ")[0] + ".pdf");

p.setEstado(EstadoPostulante.POSTULADA);

p.setFechaRegistro(System.currentTimeMillis());

p.setRegistradaPor("Carga inicial");

return p;

}

}

| **Usuario** | **Contraseña** | **Rol** |
| --- | --- | --- |
| evaluadora1 | wim2026 | EVALUADORA |
| evaluadora2 | wim2026 | EVALUADORA |
| direccion | wim2026 | DIRECCION |

Las áreas deben ser exactamente las de **Candidata.AREAS** en la app; si no, el formulario de edición no reconoce el área.

## Paso 15 · Ejecutar y probar con Postman

Ejecuta **Wim100BackendApplication** desde IntelliJ (botón Run). En la consola debe aparecer **Tomcat started on port 8080**. Revisa en Workbench que existan las tablas **postulantes** y **usuarios** con datos.

Prueba cada endpoint en Postman, con el header **Content-Type: application/json** en las peticiones con cuerpo:

| **#** | **Petición** | **Resultado esperado** |
| --- | --- | --- |
| 1 | GET http://localhost:8080/api/postulantes | 200 y la lista de 3 postulantes |
| 2 | POST /api/auth/login con el JSON de abajo | 200 con id, nombre y rol |
| 3 | POST /api/auth/login con contraseña mala | 401 |
| 4 | POST /api/postulantes con el JSON de abajo | 201 con id asignado |
| 5 | Repetir el POST anterior | 409 (correo repetido) |
| 6 | POST con "aniosExperiencia": -1 | 400 con el error del campo |
| 7 | PUT /api/postulantes/4 con datos cambiados | 200 |
| 8 | PATCH /api/postulantes/4/estado con {"estado":"EN\_EVALUACION"} | 200 |
| 9 | DELETE /api/postulantes/4 | 204; un GET posterior da 404 |

{ "usuario": "evaluadora1", "password": "wim2026" }

{

"nombre": "Antonia Pérez Lillo",

"correo": "antonia.perez@ejemplo.cl",

"telefono": "+56 9 1111 2299",

"area": "Forense y respuesta a incidentes",

"aniosExperiencia": 6,

"resumenExperiencia": "Analista forense en un equipo ficticio de respuesta a incidentes.",

"linkedinUrl": "https://www.linkedin.com/in/antonia-perez-demo",

"cvNombre": "CV\_Antonia.pdf",

"registradaPor": "Camila Rojas Demo"

}

Guarda estas peticiones en una colección de Postman y expórtala al repositorio (carpeta **postman/**). Sirve como evidencia y para la defensa.

## Paso 16 · Subir el backend a GitHub

Crea el repositorio **wim100-backend** en GitHub y sube el proyecto. El .gitignore que trae Spring Initializr ya excluye **target/** y los archivos de IntelliJ.

git init

git add .

git commit -m "feat: CRUD de postulantes y login con BCrypt"

git branch -M main

git remote add origin https://github.com/USUARIO/wim100-backend.git

git push -u origin main

Agrega un **README.md** con: requisitos, cómo crear la base de datos, usuario y contraseña de MySQL a cambiar, cómo ejecutar, tabla de endpoints y usuarios de prueba.

# Parte 3 · Errores frecuentes y checklist

## Errores frecuentes

| **Síntoma** | **Causa** | **Solución** |
| --- | --- | --- |
| "No se pudo conectar con el servidor" en el emulador | Backend apagado o URL con localhost | Levantar el backend; usar http://10.0.2.2:8080/api/ |
| CLEARTEXT communication not permitted | Android bloquea http sin TLS | android:usesCleartextTraffic="true" en el manifest |
| java.lang.SecurityException: missing INTERNET permission | Falta el permiso | uses-permission INTERNET en el manifest |
| Todo responde 401 en el backend | Se agregó spring-boot-starter-security | Quitarlo; usar solo spring-security-crypto |
| Communications link failure al arrancar Spring | MySQL apagado o credenciales malas | Iniciar MySQL; revisar usuario y contraseña en application.properties |
| Public Key Retrieval is not allowed | Configuración por defecto de MySQL 8 | allowPublicKeyRetrieval=true en la URL (ya incluido) |
| getters "cannot find symbol" | Lombok sin annotation processing | Activarlo en IntelliJ (Paso 3) |
| Las evaluaciones desaparecen al actualizar la lista | Se usó REPLACE en vez de @Upsert | Usar @Upsert (Paso 24) |
| Error 400 al guardar desde la app | Un campo no cumple las validaciones del DTO | Ver el JSON de respuesta en Logcat (okhttp) |
| Error 404 en todas las rutas | BASE\_URL sin "/api/" o sin "/" final | La URL debe terminar en /api/ |
| Test falla con "Module with the Main dispatcher is missing" | Falta Dispatchers.setMain | Ver Paso 35 |
| Dispositivo físico no conecta | Firewall o red distinta | Misma Wi-Fi, IP del PC y puerto 8080 permitido |

## Checklist contra la pauta EP4

| **Indicador** | **Qué revisar** | **Peso** |
| --- | --- | --- |
| IE 3.1.1 Interfaz | Todas las pantallas, incluida la edición, sin errores de navegación | 15% |
| IE 3.1.2 Microservicio | Backend con MySQL, CRUD y login funcionando en Postman | 15% |
| IE 3.1.3 Integración | Crear, listar, editar, eliminar y cambiar estado desde la app, reflejado en MySQL | 15% |
| IE 3.1.4 API externa | Botón "Generar nombre ficticio" funcionando con randomuser.me | 15% |
| IE 3.2.1 Pruebas | Tests de ViewModel y Repository con MockK; cobertura de la lógica de 80% o más | 15% |
| IE 3.3.1 APK firmado | app-release.apk, .jks y signingConfigs en build.gradle.kts | 10% |
| IE 3.3.2 GitHub y Trello | Dos repositorios con commits de ambos integrantes; tablero con tareas repartidas | 15% |

## Preparación para la defensa (EP5)

* Practicar el arranque completo en menos de 2 minutos: MySQL, backend, emulador y app.
* Saber explicar el recorrido de un dato: pantalla → ViewModel → Repository → Retrofit → Controller → Service → JPA → MySQL, y la vuelta.
* Diferencia entre microservicio y API externa: quién la controla, la URL base, el uso de datos propios o de terceros, y cómo se maneja su caída.
* Qué se mockea en cada test y por qué (API, DAO, historial).
* Ensayar modificaciones en vivo: agregar un campo al Postulante (entidad, DTO, app), un endpoint GET /api/postulantes/estado/{estado}, una validación nueva o un test nuevo.
