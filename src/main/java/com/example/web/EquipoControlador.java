package com.example.web;

import com.example.domain.Equipo;
import com.example.domain.SolicitudEquipo;
import com.example.domain.usuario.Usuario;
import com.example.servicio.EquipoServicio;
import com.example.servicio.SolicitudEquipoServicio;
import com.example.servicio.UsuarioServicio;
import com.example.servicio.CorreoServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    // ------------------------------------------------------------------------
    // MOSTRAR EQUIPOS (GET /equipo)
    // ------------------------------------------------------------------------

    @GetMapping
    public String mostrarEquipos(Model model, Authentication auth) {
        try {
            String nombreUsuario = auth.getName();
            Usuario usuarioActual = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);

            List<Equipo> misEquipos = equipoServicio.obtenerEquiposPorUsuario(usuarioActual.getId_usuario());

            // Obtener solicitudes pendientes
            List<SolicitudEquipo> solicitudesPendientes = solicitudEquipoServicio
                    .obtenerSolicitudesPendientesRecibidas(usuarioActual.getId_usuario());

            List<SolicitudEquipo> solicitudesEnviadas = solicitudEquipoServicio
                    .obtenerSolicitudesPendientesEnviadas(usuarioActual.getId_usuario());

            model.addAttribute("misEquipos", misEquipos);
            model.addAttribute("solicitudesPendientes", solicitudesPendientes);
            model.addAttribute("solicitudesEnviadas", solicitudesEnviadas);
            model.addAttribute("usuarioActual", usuarioActual);

            return "equipo";
        } catch (Exception e) {
            System.err.println("Error al cargar equipos: " + e.getMessage());
            model.addAttribute("mensajeError", "Error al cargar la información de equipos.");
            return "equipo";
        }
    }

    // ------------------------------------------------------------------------
    // FORMULARIO CREAR (GET /equipo/crear) - Filtrado de jugadores
    // ------------------------------------------------------------------------

    // ARVHIVO: com.example.web.EquipoControlador.java

    // ARVHIVO: com.example.web.EquipoControlador.java (Diagnóstico Final)

    @GetMapping("/crear")
    public String mostrarFormularioCrearEquipo(Model model, Authentication auth) {

        // 🔴 DEBUG 1: ¿Cuántos usuarios trae el DAO/Servicio? (DEBE SER 37)
        List<Usuario> todosLosUsuarios = usuarioServicio.listarTodos();
        System.out.println("DEBUG DAO: Usuarios totales traídos por listarTodos(): " + todosLosUsuarios.size());

        String nombreUsuario = auth.getName();
        Usuario usuarioActual = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);

        List<Integer> idsJugadoresConEquipo = equipoServicio.obtenerIdsJugadoresConEquipoActivo();

        List<Usuario> jugadoresDisponibles = todosLosUsuarios.stream()
                .filter(u -> {

                    boolean esUsuarioActual = u.getId_usuario().equals(usuarioActual.getId_usuario());
                    boolean yaTieneEquipo = idsJugadoresConEquipo.contains(u.getId_usuario());

                    // 🔴 DEBUG 2: Reintroducimos la verificación de Individuo SOLO en el log.
                    if (u.getIndividuo() == null && !esUsuarioActual) {
                        System.out.println("DEBUG FILTRO: Usuario ID " + u.getId_usuario() + " excluido por Individuo NULO.");
                    }

                    return !esUsuarioActual && !yaTieneEquipo && u.getIndividuo() != null; // ⬅️ DEBEMOS VOLVER A PONER ESTE FILTRO
                })
                .collect(Collectors.toList());

        // 🔴 DEBUG 3: ¿Cuántos usuarios quedan al final? (DEBE SER 36)
        System.out.println("DEBUG FILTRO: Usuarios restantes (después de excluir filtros): " + jugadoresDisponibles.size());

        model.addAttribute("equipo", new Equipo());
        model.addAttribute("jugadoresDisponibles", jugadoresDisponibles);
        model.addAttribute("usuarioActual", usuarioActual);

        return "equipo-crear";
    }

    // ------------------------------------------------------------------------
    // CREAR EQUIPO (POST /equipo/crear) - Enviar Solicitud
    // ------------------------------------------------------------------------

    @PostMapping("/crear")
    public String crearEquipo(
            @RequestParam("nombreEquipo") String nombreEquipo,
            @RequestParam("idJugador2") Long idJugador2,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            String nombreUsuario = auth.getName();
            Usuario jugador1 = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);

            // CORRECCIÓN 1: Usar el método de búsqueda por ID (Long)
            Usuario jugador2 = usuarioServicio.obtenerUsuarioPorId(idJugador2);

            if (jugador2 == null || jugador2.isEliminado()) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "El jugador seleccionado no está disponible.");
                return "redirect:/equipo/crear";
            }

            // ⚠️ ATENCIÓN: Si permitiste invitar a jugadores que ya tienen equipo,
            // la lógica aquí (en SolicitudEquipoServicio) DEBE manejar
            // si el jugador2 ya tiene un equipo ACTIVO y si la aplicación lo permite.

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

    // ------------------------------------------------------------------------
    // ACEPTAR SOLICITUD (POST /solicitud/{idSolicitud}/aceptar)
    // ------------------------------------------------------------------------

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

            // CORRECCIÓN 2: Usamos el método de negocio para ACEPTAR y CREAR el equipo
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

    // ------------------------------------------------------------------------
    // RECHAZAR SOLICITUD (POST /solicitud/{idSolicitud}/rechazar)
    // ------------------------------------------------------------------------

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

            // CORRECCIÓN 3: Usamos el método de negocio para RECHAZAR
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

    // ------------------------------------------------------------------------
    // CANCELAR SOLICITUD (POST /solicitud/{idSolicitud}/cancelar)
    // ------------------------------------------------------------------------

    @PostMapping("/solicitud/{idSolicitud}/cancelar")
    public String cancelarSolicitud(
            @PathVariable("idSolicitud") Long idSolicitud,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            String nombreUsuario = auth.getName();
            Usuario jugador1 = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);

            SolicitudEquipo solicitud = solicitudEquipoServicio.obtenerSolicitudPorId(idSolicitud);

            if (solicitud == null || !solicitud.getEstado().equals("PENDIENTE")) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "La solicitud ya no está disponible.");
                return "redirect:/equipo";
            }

            if (!solicitud.getJugador1().getId_usuario().equals(jugador1.getId_usuario())) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "No tienes permiso para cancelar esta solicitud.");
                return "redirect:/equipo";
            }

            solicitudEquipoServicio.eliminarSolicitud(idSolicitud);

            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Solicitud cancelada exitosamente.");

            return "redirect:/equipo";

        } catch (Exception e) {
            System.err.println("Error al cancelar solicitud: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Error al cancelar la solicitud: " + e.getMessage());
            return "redirect:/equipo";
        }
    }

    // ------------------------------------------------------------------------
    // VER DETALLES DE EQUIPO (GET /{idEquipo})
    // ------------------------------------------------------------------------

    @GetMapping("/{idEquipo}")
    public String verDetallesEquipo(
            @PathVariable("idEquipo") Long idEquipo,
            Model model,
            Authentication auth) {

        try {
            Equipo equipo = equipoServicio.obtenerEquipoPorId(idEquipo);

            if (equipo == null) {
                model.addAttribute("mensajeError", "Equipo no encontrado.");
                return "redirect:/equipo";
            }

            String nombreUsuario = auth.getName();
            Usuario usuarioActual = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);

            if (!equipo.getJugador1().getId_usuario().equals(usuarioActual.getId_usuario()) &&
                    !equipo.getJugador2().getId_usuario().equals(usuarioActual.getId_usuario())) {
                model.addAttribute("mensajeError", "No tienes acceso a este equipo.");
                return "redirect:/equipo";
            }

            model.addAttribute("equipo", equipo);
            model.addAttribute("usuarioActual", usuarioActual);

            return "equipo-detalles";

        } catch (Exception e) {
            System.err.println("Error al cargar detalles del equipo: " + e.getMessage());
            model.addAttribute("mensajeError", "Error al cargar la información del equipo.");
            return "redirect:/equipo";
        }
    }

    // ------------------------------------------------------------------------
    // ELIMINAR EQUIPO (POST /{idEquipo}/eliminar)
    // ------------------------------------------------------------------------

    @PostMapping("/{idEquipo}/eliminar")
    public String eliminarEquipo(
            @PathVariable("idEquipo") Long idEquipo,
            Authentication auth,
            RedirectAttributes redirectAttributes) {

        try {
            Equipo equipo = equipoServicio.obtenerEquipoPorId(idEquipo);

            if (equipo == null) {
                redirectAttributes.addFlashAttribute("mensajeError", "Equipo no encontrado.");
                return "redirect:/equipo";
            }

            String nombreUsuario = auth.getName();
            Usuario usuarioActual = usuarioServicio.localizarPorNombreUsuario(nombreUsuario);

            if (!equipo.getJugador1().getId_usuario().equals(usuarioActual.getId_usuario()) &&
                    !equipo.getJugador2().getId_usuario().equals(usuarioActual.getId_usuario())) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "No tienes permiso para eliminar este equipo.");
                return "redirect:/equipo";
            }

            Usuario otroMiembro = equipo.getJugador1().getId_usuario().equals(usuarioActual.getId_usuario())
                    ? equipo.getJugador2()
                    : equipo.getJugador1();

            String asunto = "Equipo Disuelto - PadelApp";
            String mensaje = String.format(
                    "Hola %s,\n\n" +
                            "El equipo '%s' ha sido disuelto por %s.\n\n" +
                            "Saludos,\n" +
                            "Equipo PadelApp",
                    otroMiembro.getIndividuo().getNombre(),
                    equipo.getNombreEquipo(),
                    usuarioActual.getIndividuo().getNombre() + " " +
                            usuarioActual.getIndividuo().getApellido()
            );

            correoServicio.enviarCorreoIndividual(otroMiembro, asunto, mensaje);

            equipoServicio.eliminarEquipo(idEquipo);

            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Equipo '" + equipo.getNombreEquipo() + "' eliminado exitosamente.");

            return "redirect:/equipo";

        } catch (Exception e) {
            System.err.println("Error al eliminar equipo: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Error al eliminar el equipo: " + e.getMessage());
            return "redirect:/equipo";
        }
    }
}