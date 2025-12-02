package com.example.web;

import com.example.domain.usuario.Usuario;
import com.example.servicio.CorreoServicio;
import com.example.servicio.UsuarioServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/correo")
public class ControladorCorreo {

    @Autowired
    private CorreoServicio correoServicio;

    @Autowired
    private UsuarioServicio usuarioServicio;

    /**
     * Mostrar formulario de envío de correos
     */
    @GetMapping("/enviar")
    public String mostrarFormularioCorreo(Model model) {
        try {
            // Obtener cantidad de jugadores activos para mostrar en el formulario
            List<Usuario> jugadoresActivos = usuarioServicio.listarTodos()
                    .stream()
                    .filter(usuario -> !usuario.isEliminado() &&
                            usuario.getIndividuo() != null &&
                            !usuario.getIndividuo().isEliminado())
                    .collect(Collectors.toList());

            model.addAttribute("totalJugadores", jugadoresActivos.size());
            model.addAttribute("jugadores", jugadoresActivos);

            return "enviar-correo";
        } catch (Exception e) {
            System.err.println("Error al cargar formulario de correo: " + e.getMessage());
            model.addAttribute("error", "Error al cargar la página de correos");
            return "redirect:/jugadores?error=errorCorreo";
        }
    }

    /**
     * Enviar correo masivo a todos los jugadores activos
     */
    @PostMapping("/masivo")
    public String enviarCorreoMasivo(@RequestParam String asunto,
                                     @RequestParam String mensaje,
                                     @RequestParam(required = false, defaultValue = "GENERAL") String tipoEvento,
                                     RedirectAttributes redirectAttributes,
                                     Authentication authentication) {
        try {
            // Validaciones básicas
            if (asunto == null || asunto.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "El asunto del correo no puede estar vacío.");
                return "redirect:/jugadores";
            }

            if (mensaje == null || mensaje.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "El mensaje del correo no puede estar vacío.");
                return "redirect:/jugadores";
            }

            // Obtener jugadores activos
            List<Usuario> jugadores = usuarioServicio.listarTodos()
                    .stream()
                    .filter(usuario -> !usuario.isEliminado() &&
                            usuario.getIndividuo() != null &&
                            !usuario.getIndividuo().isEliminado() &&
                            usuario.getIndividuo().getCorreo() != null &&
                            !usuario.getIndividuo().getCorreo().trim().isEmpty())
                    .collect(Collectors.toList());

            if (jugadores.isEmpty()) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "No hay jugadores activos con correos válidos para enviar.");
                return "redirect:/jugadores";
            }

            // Log de la acción
            String usuarioActual = authentication != null ? authentication.getName() : "Sistema";
            System.out.println("Usuario " + usuarioActual + " enviando correos masivos:");
            System.out.println("- Asunto: " + asunto);
            System.out.println("- Destinatarios: " + jugadores.size());
            System.out.println("- Tipo de evento: " + tipoEvento);

            // Enviar correos de forma asíncrona
            CompletableFuture<Void> futureEnvio = correoServicio.enviarCorreoMasivo(
                    jugadores, asunto.trim(), mensaje.trim(), tipoEvento);

            // Mensaje de éxito
            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Correos enviándose en segundo plano. Se notificará a " +
                            jugadores.size() + " jugadores con correos válidos.");

        } catch (Exception e) {
            System.err.println("Error al enviar correos masivos: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Error al enviar correos: " + e.getMessage());
        }

        return "redirect:/jugadores";
    }


    @PostMapping("/individual/{idUsuario}")
    public String enviarCorreoIndividual(@PathVariable("idUsuario") Integer idUsuario,
                                         @RequestParam String asunto,
                                         @RequestParam String mensaje,
                                         RedirectAttributes redirectAttributes,
                                         Authentication authentication) {
        try {
            Usuario usuario = usuarioServicio.encontrarPorId(idUsuario);

            if (usuario == null || usuario.isEliminado() ||
                    usuario.getIndividuo() == null || usuario.getIndividuo().isEliminado()) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "No se pudo encontrar el jugador especificado.");
                return "redirect:/jugadores";
            }

            if (usuario.getIndividuo().getCorreo() == null ||
                    usuario.getIndividuo().getCorreo().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "El jugador no tiene un correo válido registrado.");
                return "redirect:/jugadores";
            }

            // Log de la acción
            String usuarioActual = authentication != null ? authentication.getName() : "Sistema";
            System.out.println("Usuario " + usuarioActual + " enviando correo individual:");
            System.out.println("- Destinatario: " + usuario.getIndividuo().getNombre());
            System.out.println("- Correo: " + usuario.getIndividuo().getCorreo());
            System.out.println("- Asunto: " + asunto);

            // Enviar correo individual
            CompletableFuture<Boolean> resultado = correoServicio.enviarCorreoIndividual(
                    usuario, asunto.trim(), mensaje.trim());

            redirectAttributes.addFlashAttribute("mensajeExito",
                    "Correo enviado a " + usuario.getIndividuo().getNombre());

        } catch (Exception e) {
            System.err.println("Error al enviar correo individual: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Error al enviar correo: " + e.getMessage());
        }

        return "redirect:/jugadores";
    }

    /**
     * Obtener estadísticas de correos enviados
     */
    @GetMapping("/estadisticas")
    @ResponseBody
    public Object obtenerEstadisticasCorreos() {
        try {
            return correoServicio.obtenerEstadisticasEnvio();
        } catch (Exception e) {
            System.err.println("Error al obtener estadísticas de correos: " + e.getMessage());
            return "{\"error\": \"No se pudieron cargar las estadísticas\"}";
        }
    }

    /**
     * Verificar estado del servicio de correos
     */
    @GetMapping("/estado")
    @ResponseBody
    public String verificarEstadoServicio() {
        try {
            boolean disponible = correoServicio.verificarConexion();
            return disponible ? "ACTIVO" : "INACTIVO";
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Previsualizar correo antes de enviarlo
     */
    @PostMapping("/previsualizar")
    @ResponseBody
    public Object previsualizarCorreo(@RequestParam String asunto,
                                      @RequestParam String mensaje,
                                      @RequestParam(required = false, defaultValue = "GENERAL") String tipoEvento) {
        try {
            return correoServicio.generarPrevisualizacion(asunto, mensaje, tipoEvento);
        } catch (Exception e) {
            System.err.println("Error al generar previsualización: " + e.getMessage());
            return "{\"error\": \"Error al generar previsualización\"}";
        }
    }

    /**
     * Cancelar envíos pendientes (si es posible)
     */
    @PostMapping("/cancelar")
    public String cancelarEnvios(RedirectAttributes redirectAttributes,
                                 Authentication authentication) {
        try {
            String usuarioActual = authentication != null ? authentication.getName() : "Sistema";
            System.out.println("Usuario " + usuarioActual + " solicitó cancelar envíos pendientes");

            boolean cancelado = correoServicio.cancelarEnviosPendientes();

            if (cancelado) {
                redirectAttributes.addFlashAttribute("mensajeExito",
                        "Envíos pendientes cancelados correctamente.");
            } else {
                redirectAttributes.addFlashAttribute("mensajeError",
                        "No hay envíos pendientes para cancelar.");
            }

        } catch (Exception e) {
            System.err.println("Error al cancelar envíos: " + e.getMessage());
            redirectAttributes.addFlashAttribute("mensajeError",
                    "Error al cancelar envíos: " + e.getMessage());
        }

        return "redirect:/jugadores";
    }
}