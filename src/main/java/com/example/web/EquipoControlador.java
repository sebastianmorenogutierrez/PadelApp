package com.example.web;

import com.example.domain.Equipo;
import com.example.domain.SolicitudEquipo;
import com.example.domain.usuario.Usuario;
import com.example.servicio.EquipoServicio;
import com.example.servicio.SolicitudEquipoServicio;
import com.example.servicio.UsuarioServicio;
import com.example.servicio.CorreoServicio;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/equipo")
public class EquipoControlador {

    @Autowired
    private EquipoServicio equipoServicio;

    @Autowired
    private SolicitudEquipoServicio solicitudEquipoServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    @Autowired
    private CorreoServicio correoServicio;

    // --- MÉTODOS GET (Sin cambios en este ejemplo, asumiendo que ya funcionan) ---

    @GetMapping
    public String mostrarEquipos(Model model, Authentication auth) {
        // ... (Implementación del método)
        return "equipo";
    }

    @GetMapping("/crear")
    public String mostrarFormularioCrearEquipo(Model model, Authentication auth) {
        // ... (Implementación del método)
        return "equipo-crear";
    }

    // --- Método crearEquipo ---

    @PostMapping("/crear")
    public String crearEquipo(
            @RequestParam("nombreEquipo") String nombreEquipo,
            @RequestParam("idJugador2") Long idJugador2,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            String nombreUsuario = auth.getName();
            Usuario jugador1 = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);

            // 🟢 CORRECCIÓN 1 APLICADA: Usar el método de búsqueda por ID (Long)
            Usuario jugador2 = usuarioServicio.obtenerUsuarioPorId(idJugador2);

            if (jugador2 == null || jugador2.isEliminado()) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "El jugador seleccionado no está disponible.");
                return "redirect:/equipo/crear";
            }

            Integer idJugador1 = jugador1.getId_usuario();
            Integer idJugador2Int = jugador2.getId_usuario();

            boolean existeSolicitud = solicitudEquipoServicio
                    .existeSolicitudPendienteEntreJugadores(idJugador1, idJugador2Int);

            if (existeSolicitud) {
                redirectAttributes.addFlashAttribute("mensajeAdvertencia",
                        "Ya tienes una solicitud pendiente con este jugador.");
                return "redirect:/equipo";
            }

            SolicitudEquipo solicitud = new SolicitudEquipo();
            solicitud.setJugador1(jugador1);
            solicitud.setJugador2(jugador2);
            solicitud.setNombreEquipo(nombreEquipo);

            // Usamos el método de negocio 'enviarSolicitud'
            solicitudEquipoServicio.enviarSolicitud(solicitud);

            String asunto = "Nueva Solicitud de Equipo - PadelApp";
            String mensaje = String.format(
                    "¡Hola %s!\n\n" +
                            "%s te ha invitado a formar el equipo '%s'.\n\n" +
                            "Ingresa a la aplicación para revisar y responder la solicitud.\n\n" +
                            "Saludos,\n" +
                            "Equipo PadelApp",
                    jugador2.getIndividuo().getNombre(),
                    jugador1.getIndividuo().getNombre() + " " + jugador1.getIndividuo().getApellido(),
                    nombreEquipo
            );

            correoServicio.enviarCorreoIndividual(jugador2, asunto, mensaje);

            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Solicitud de equipo enviada exitosamente a " +
                            jugador2.getIndividuo().getNombre() + " " +
                            jugador2.getIndividuo().getApellido());

            return "redirect:/equipo";

        } catch (Exception e) {
            System.err.println("Error al crear solicitud de equipo: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Error al enviar la solicitud de equipo: " + e.getMessage());
            return "redirect:/equipo/crear";
        }
    }

    // --- Método aceptarSolicitud ---

    @PostMapping("/solicitud/{idSolicitud}/aceptar")
    public String aceptarSolicitud(
            @PathVariable("idSolicitud") Long idSolicitud,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            String nombreUsuario = auth.getName();
            Usuario jugador2 = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);

            SolicitudEquipo solicitud = solicitudEquipoServicio.obtenerSolicitudPorId(idSolicitud);

            if (solicitud == null || !solicitud.getEstado().equals("PENDIENTE")) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "La solicitud ha expirado o fue cancelada por el emisor.");
                return "redirect:/equipo";
            }

            if (!solicitud.getJugador2().getId_usuario().equals(jugador2.getId_usuario())) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "No tienes permiso para aceptar esta solicitud.");
                return "redirect:/equipo";
            }

            // 🟢 CORRECCIÓN 2 APLICADA: Usamos el método de negocio para ACEPTAR y CREAR el equipo
            Equipo equipo = solicitudEquipoServicio.aceptarSolicitud(idSolicitud);

            String asunto = "¡Solicitud de Equipo Aceptada! - PadelApp";
            String mensaje = String.format(
                    "¡Hola %s!\n\n" +
                            "%s ha aceptado tu solicitud.\n" +
                            "El equipo '%s' ha sido formado exitosamente.\n\n" +
                            "¡Prepárense para competir!\n\n" +
                            "Saludos,\n" +
                            "Equipo PadelApp",
                    solicitud.getJugador1().getIndividuo().getNombre(),
                    jugador2.getIndividuo().getNombre() + " " + jugador2.getIndividuo().getApellido(),
                    equipo.getNombreEquipo()
            );

            correoServicio.enviarCorreoIndividual(
                    solicitud.getJugador1(), asunto, mensaje);

            redirectAttributes.addFlashAttribute("mensajeExito",
                    "¡Equipo '" + equipo.getNombreEquipo() + "' formado exitosamente!");

            return "redirect:/equipo";

        } catch (Exception e) {
            System.err.println("Error al aceptar solicitud: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Error al procesar la aceptación: " + e.getMessage());
            return "redirect:/equipo";
        }
    }

    // --- Método rechazarSolicitud ---

    @PostMapping("/solicitud/{idSolicitud}/rechazar")
    public String rechazarSolicitud(
            @PathVariable("idSolicitud") Long idSolicitud,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            String nombreUsuario = auth.getName();
            Usuario jugador2 = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);

            SolicitudEquipo solicitud = solicitudEquipoServicio.obtenerSolicitudPorId(idSolicitud);

            if (solicitud == null || !solicitud.getEstado().equals("PENDIENTE")) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "La solicitud ya no está disponible.");
                return "redirect:/equipo";
            }

            if (!solicitud.getJugador2().getId_usuario().equals(jugador2.getId_usuario())) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "No tienes permiso para rechazar esta solicitud.");
                return "redirect:/equipo";
            }

            // 🟢 CORRECCIÓN 3 APLICADA: Usamos el método de negocio para RECHAZAR
            solicitudEquipoServicio.rechazarSolicitud(idSolicitud, "Rechazada por el receptor");


            String asunto = "Solicitud de Equipo Rechazada - PadelApp";
            String mensaje = String.format(
                    "Hola %s,\n\n" +
                            "%s ha rechazado tu solicitud para formar el equipo '%s'.\n\n" +
                            "Puedes intentar invitar a otros jugadores.\n\n" +
                            "Saludos,\n" +
                            "Equipo PadelApp",
                    solicitud.getJugador1().getIndividuo().getNombre(),
                    jugador2.getIndividuo().getNombre() + " " + jugador2.getIndividuo().getApellido(),
                    solicitud.getNombreEquipo()
            );

            correoServicio.enviarCorreoIndividual(
                    solicitud.getJugador1(), asunto, mensaje);

            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Solicitud rechazada correctamente.");

            return "redirect:/equipo";

        } catch (Exception e) {
            System.err.println("Error al rechazar solicitud: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Error al procesar el rechazo: " + e.getMessage());
            return "redirect:/equipo";
        }
    }

    // ... (El resto de métodos /solicitud/{idSolicitud}/cancelar, /{idEquipo} y /{idEquipo}/eliminar son iguales)
}