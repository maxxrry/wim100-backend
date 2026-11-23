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

/** Carga datos de prueba ficticios si las tablas están vacías. */
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
        // Las áreas son exactamente las de Candidata.ESPECIALIDADES en la app WIM100
        if (postulantes.count() == 0) {
            postulantes.saveAll(List.of(
                    nueva("Ignacia Valdés Soto", "ignacia.valdes@ejemplo.cl", "+56 9 1111 2201",
                            "Pentesting", 9,
                            "Pentester líder en una consultora ficticia. Coordina ejercicios de Red Team.",
                            "https://www.linkedin.com/in/ignacia-valdes-demo"),
                    nueva("Martina Herrera Lagos", "martina.herrera@ejemplo.cl", "+56 9 1111 2202",
                            "Operaciones SOC", 7,
                            "Jefa de un SOC ficticio. Diseñó playbooks de detección de incidentes.",
                            "https://www.linkedin.com/in/martina-herrera-demo"),
                    nueva("Fernanda Castillo Rivas", "fernanda.castillo@ejemplo.cl", "+56 9 1111 2203",
                            "Cumplimiento normativo", 12,
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
        p.setCvNombre("CV_" + nombre.split(" ")[0] + ".pdf");
        p.setEstado(EstadoPostulante.RECIBIDA);
        p.setFechaRegistro(System.currentTimeMillis());
        p.setRegistradaPor("Carga inicial");
        return p;
    }
}
